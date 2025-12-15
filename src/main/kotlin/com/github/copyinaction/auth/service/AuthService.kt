package com.github.copyinaction.auth.service

import com.github.copyinaction.auth.domain.EmailVerificationToken
import com.github.copyinaction.auth.dto.AuthTokenInfo
import com.github.copyinaction.auth.dto.LoginRequest
import com.github.copyinaction.auth.dto.SignupRequest
import com.github.copyinaction.auth.repository.EmailVerificationTokenRepository
import com.github.copyinaction.auth.repository.UserRepository
import com.github.copyinaction.common.service.EmailService
import com.github.copyinaction.config.jwt.JwtTokenProvider
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.auth.domain.User
import com.github.copyinaction.repository.RefreshTokenRepository
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val emailVerificationTokenRepository: EmailVerificationTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManagerBuilder: AuthenticationManagerBuilder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val emailService: EmailService
) {

    @Transactional
    fun signup(request: SignupRequest): User {
        if (userRepository.findByEmail(request.email).isPresent) {
            throw CustomException(ErrorCode.EMAIL_ALREADY_EXISTS)
        }

        val confirmedToken = emailVerificationTokenRepository.findByEmailAndIsConfirmed(request.email, true)
            .orElseThrow { CustomException(ErrorCode.EMAIL_NOT_VERIFIED) } // Or a more specific error like OTP_NOT_CONFIRMED

        if (confirmedToken.isExpired()) {
            throw CustomException(ErrorCode.EXPIRED_EMAIL_VERIFICATION_TOKEN)
        }

        emailVerificationTokenRepository.delete(confirmedToken) // Consume the token after successful signup

        val sanitizedPhoneNumber = request.phoneNumber.replace("-", "")

        val user = User.create(
            email = request.email,
            username = request.username,
            rawPassword = request.password,
            passwordEncoder = passwordEncoder,
            phoneNumber = sanitizedPhoneNumber,
            isEmailVerified = true
        )
        return userRepository.save(user)
    }

    @Transactional
    fun login(request: LoginRequest): AuthTokenInfo {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { CustomException(ErrorCode.LOGIN_FAILED) }

        if (!user.isEmailVerified) {
            throw CustomException(ErrorCode.EMAIL_NOT_VERIFIED)
        }

        val authenticationToken = UsernamePasswordAuthenticationToken(request.email, request.password)
        val authentication = authenticationManagerBuilder.`object`.authenticate(authenticationToken)
        val accessToken = jwtTokenProvider.createAccessToken(authentication)

        // User(Aggregate Root)가 RefreshToken 발급 관리
        val refreshToken = user.issueRefreshToken(jwtTokenProvider)
        userRepository.save(user)

        return AuthTokenInfo(
            accessToken = accessToken,
            refreshToken = refreshToken.token,
            accessTokenExpiresIn = jwtTokenProvider.getAccessTokenValidityInSeconds()
        )
    }

    @Transactional
    fun refresh(refreshTokenString: String): AuthTokenInfo {
        val refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
            .orElseThrow { CustomException(ErrorCode.INVALID_REFRESH_TOKEN) }

        val user = refreshToken.user

        // User(Aggregate Root)가 토큰 갱신 관리
        val newRefreshToken = user.rotateRefreshToken(refreshToken, jwtTokenProvider)
        userRepository.save(user)

        val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
        val authentication = UsernamePasswordAuthenticationToken(user.email, null, authorities)
        val newAccessToken = jwtTokenProvider.createAccessToken(authentication)

        return AuthTokenInfo(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken.token,
            accessTokenExpiresIn = jwtTokenProvider.getAccessTokenValidityInSeconds()
        )
    }

    @Transactional
    fun requestEmailVerification(email: String) {
        if (userRepository.findByEmail(email).isPresent) {
            throw CustomException(ErrorCode.EMAIL_ALREADY_EXISTS)
        }

        // 기존 토큰 삭제 후 flush하여 unique 제약 충돌 방지
        emailVerificationTokenRepository.deleteByEmail(email)
        emailVerificationTokenRepository.flush()

        val verificationToken = EmailVerificationToken.create(email)
        emailVerificationTokenRepository.save(verificationToken)

        emailService.sendVerificationEmail(email, verificationToken.token)
    }

    @Transactional
    fun confirmOtp(email: String, otp: String) {
        val verificationToken = emailVerificationTokenRepository.findByEmailAndToken(email, otp)
            .orElseThrow { CustomException(ErrorCode.INVALID_EMAIL_VERIFICATION_TOKEN) }

        verificationToken.validate(email, otp) // Validate for expiry, email, and if already confirmed
        verificationToken.confirm() // Mark as confirmed
        emailVerificationTokenRepository.save(verificationToken)
    }
}