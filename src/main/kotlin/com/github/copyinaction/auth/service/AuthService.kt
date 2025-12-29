package com.github.copyinaction.auth.service

import com.github.copyinaction.audit.domain.AuditAction
import com.github.copyinaction.audit.service.AuditLogService
import com.github.copyinaction.auth.domain.EmailVerificationToken
import com.github.copyinaction.auth.dto.AuthTokenInfo
import com.github.copyinaction.auth.dto.LoginRequest
import com.github.copyinaction.auth.dto.LoginResponse
import com.github.copyinaction.auth.dto.SignupRequest
import com.github.copyinaction.auth.dto.UserResponse
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
    private val emailService: EmailService,
    private val auditLogService: AuditLogService
) {

    @Transactional
    fun signup(request: SignupRequest): User {
        if (userRepository.findByEmail(request.email).isPresent) {
            throw CustomException(ErrorCode.EMAIL_ALREADY_EXISTS)
        }

        val confirmedToken = emailVerificationTokenRepository.findByEmailAndIsConfirmed(request.email, true)
            .orElseThrow { CustomException(ErrorCode.EMAIL_NOT_VERIFIED) }

        if (confirmedToken.isExpired()) {
            throw CustomException(ErrorCode.EXPIRED_EMAIL_VERIFICATION_TOKEN)
        }

        emailVerificationTokenRepository.delete(confirmedToken)

        val user = User.create(
            email = request.email,
            username = request.username,
            rawPassword = request.password,
            passwordEncoder = passwordEncoder,
            phoneNumber = request.phoneNumber.replace("-", ""),
            isEmailVerified = true
        )
        val savedUser = userRepository.save(user)

        auditLogService.saveAuthEvent(
            action = AuditAction.SIGNUP,
            email = request.email,
            success = true,
            userId = savedUser.id,
            userRole = savedUser.role
        )

        return savedUser
    }

    @Transactional
    fun login(request: LoginRequest): LoginResponse {
        val user = authenticateUser(request)
        val tokenInfo = issueTokens(user)

        auditLogService.saveAuthEvent(
            action = AuditAction.LOGIN,
            email = request.email,
            success = true,
            userId = user.id,
            userRole = user.role
        )

        return LoginResponse(token = tokenInfo, user = UserResponse.from(user))
    }

    private fun authenticateUser(request: LoginRequest): User {
        try {
            val user = userRepository.findByEmail(request.email)
                .orElseThrow { CustomException(ErrorCode.LOGIN_FAILED) }

            if (!user.isEmailVerified) {
                throw CustomException(ErrorCode.EMAIL_NOT_VERIFIED)
            }

            val authToken = UsernamePasswordAuthenticationToken(request.email, request.password)
            authenticationManagerBuilder.`object`.authenticate(authToken)

            return user
        } catch (e: Exception) {
            auditLogService.saveAuthEvent(
                action = AuditAction.LOGIN_FAILED,
                email = request.email,
                success = false
            )
            throw e
        }
    }

    private fun issueTokens(user: User): AuthTokenInfo {
        val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
        val principal = CustomUserDetails(user.id, user.email, "", authorities)
        val authentication = UsernamePasswordAuthenticationToken(principal, null, authorities)

        val accessToken = jwtTokenProvider.createAccessToken(authentication)
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
        val newRefreshToken = user.rotateRefreshToken(refreshToken, jwtTokenProvider)
        userRepository.save(user)

        val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
        val principal = CustomUserDetails(user.id, user.email, "", authorities)
        val authentication = UsernamePasswordAuthenticationToken(principal, null, authorities)
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

        verificationToken.validate(email, otp)
        verificationToken.confirm()
        emailVerificationTokenRepository.save(verificationToken)
    }

    @Transactional(readOnly = true)
    fun getUserById(email: String): User {
        return userRepository.findByEmail(email)
            .orElseThrow { CustomException(ErrorCode.USER_NOT_FOUND) }
    }
}
