package com.github.copyinaction.auth.service

import com.github.copyinaction.auth.domain.EmailVerificationToken
import com.github.copyinaction.auth.domain.RefreshToken
import com.github.copyinaction.auth.domain.User
import com.github.copyinaction.auth.dto.AuthTokenInfo
import com.github.copyinaction.auth.dto.LoginRequest
import com.github.copyinaction.auth.dto.SignupRequest
import com.github.copyinaction.auth.repository.EmailVerificationTokenRepository
import com.github.copyinaction.auth.repository.UserRepository
import com.github.copyinaction.common.service.EmailService // Added import
import com.github.copyinaction.config.jwt.JwtTokenProvider
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.repository.RefreshTokenRepository
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val emailVerificationTokenRepository: EmailVerificationTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManagerBuilder: AuthenticationManagerBuilder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val emailService: EmailService // Added EmailService to constructor
) {

    @Transactional
    fun signup(request: SignupRequest): User {
        if (userRepository.findByEmail(request.email).isPresent) {
            throw CustomException(ErrorCode.EMAIL_ALREADY_EXISTS)
        }
        val user = request.toEntity(passwordEncoder)
        return userRepository.save(user)
    }

    @Transactional
    fun login(request: LoginRequest): AuthTokenInfo {
        val authenticationToken = UsernamePasswordAuthenticationToken(request.email, request.password)
        val authentication = authenticationManagerBuilder.`object`.authenticate(authenticationToken)
        val accessToken = jwtTokenProvider.createAccessToken(authentication)

        // 사용자 조회
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { CustomException(ErrorCode.LOGIN_FAILED) }

        // 기존 리프레시 토큰 삭제
        refreshTokenRepository.deleteByUserId(user.id)

        // 새 리프레시 토큰 생성 및 저장
        val refreshToken = RefreshToken(
            user = user,
            token = jwtTokenProvider.createRefreshToken(),
            expiryDate = jwtTokenProvider.getRefreshTokenExpiryDate()
        )
        refreshTokenRepository.save(refreshToken)

        return AuthTokenInfo(
            accessToken = accessToken,
            refreshToken = refreshToken.token,
            accessTokenExpiresIn = jwtTokenProvider.getAccessTokenValidityInSeconds()
        )
    }

    @Transactional
    fun refresh(refreshTokenString: String): AuthTokenInfo {
        // 리프레시 토큰 조회
        val refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
            .orElseThrow { CustomException(ErrorCode.INVALID_REFRESH_TOKEN) }

        // 만료 확인
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken)
            throw CustomException(ErrorCode.EXPIRED_REFRESH_TOKEN)
        }

        val user = refreshToken.user

        // 새 액세스 토큰 생성
        val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
        val authentication = UsernamePasswordAuthenticationToken(user.email, null, authorities)
        val newAccessToken = jwtTokenProvider.createAccessToken(authentication)

        // 기존 리프레시 토큰 삭제 및 새로 발급 (Rotation)
        refreshTokenRepository.delete(refreshToken)
        val newRefreshToken = RefreshToken(
            user = user,
            token = jwtTokenProvider.createRefreshToken(),
            expiryDate = jwtTokenProvider.getRefreshTokenExpiryDate()
        )
        refreshTokenRepository.save(newRefreshToken)

        return AuthTokenInfo(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken.token,
            accessTokenExpiresIn = jwtTokenProvider.getAccessTokenValidityInSeconds()
        )
    }

    @Transactional
    fun requestEmailVerification(email: String) {
        val user = userRepository.findByEmail(email)
            .orElseThrow { CustomException(ErrorCode.EMAIL_NOT_FOUND) }

        if (user.isEmailVerified) {
            throw CustomException(ErrorCode.EMAIL_ALREADY_VERIFIED)
        }

        // 기존 토큰이 있다면 삭제 (새로운 토큰 발급을 위함)
        emailVerificationTokenRepository.findByUserId(user.id).ifPresent {
            emailVerificationTokenRepository.delete(it)
        }

        val token = UUID.randomUUID().toString()
        val expiryDate = LocalDateTime.now().plusMinutes(30) // 30분 유효

        val emailVerificationToken = EmailVerificationToken(
            user = user,
            token = token,
            expiryDate = expiryDate
        )
        emailVerificationTokenRepository.save(emailVerificationToken)

        // 실제 이메일 전송 로직
        emailService.sendVerificationEmail(user.email, token)
    }

    @Transactional
    fun confirmEmailVerification(token: String) {
        val verificationToken = emailVerificationTokenRepository.findByToken(token)
            .orElseThrow { CustomException(ErrorCode.INVALID_EMAIL_VERIFICATION_TOKEN) }

        if (verificationToken.isExpired()) {
            emailVerificationTokenRepository.delete(verificationToken) // 만료된 토큰은 삭제
            throw CustomException(ErrorCode.EXPIRED_EMAIL_VERIFICATION_TOKEN)
        }

        val user = verificationToken.user
        if (user.isEmailVerified) {
            throw CustomException(ErrorCode.EMAIL_ALREADY_VERIFIED)
        }

        user.isEmailVerified = true
        userRepository.save(user)
        emailVerificationTokenRepository.delete(verificationToken) // 인증 완료 후 토큰 삭제
    }
}