package com.github.copyinaction.auth.service

import com.github.copyinaction.audit.domain.AuditAction
import com.github.copyinaction.audit.service.AuditLogService
import com.github.copyinaction.auth.dto.AuthTokenInfo
import com.github.copyinaction.auth.dto.LoginRequest
import com.github.copyinaction.auth.dto.LoginResponse
import com.github.copyinaction.auth.dto.SignupRequest
import com.github.copyinaction.auth.dto.UserResponse
import com.github.copyinaction.auth.repository.UserRepository
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.auth.domain.User
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManagerBuilder: AuthenticationManagerBuilder,
    private val tokenService: TokenService,
    private val emailVerificationService: EmailVerificationService,
    private val auditLogService: AuditLogService
) {

    @Transactional
    fun signup(request: SignupRequest): User {
        if (userRepository.findByEmail(request.email).isPresent) {
            throw CustomException(ErrorCode.EMAIL_ALREADY_EXISTS)
        }

        // 이메일 인증 완료 여부 검증 및 토큰 삭제
        emailVerificationService.validateAndConsumeVerification(request.email)

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
        val tokenInfo = tokenService.issueTokens(user)
        userRepository.save(user) // RefreshToken 저장

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

    @Transactional
    fun refresh(refreshTokenString: String): AuthTokenInfo {
        return tokenService.refresh(refreshTokenString)
    }

    @Transactional
    fun requestEmailVerification(email: String) {
        emailVerificationService.requestVerification(email)
    }

    @Transactional
    fun confirmOtp(email: String, otp: String) {
        emailVerificationService.confirmOtp(email, otp)
    }

    @Transactional(readOnly = true)
    fun getUserById(email: String): User {
        return userRepository.findByEmail(email)
            .orElseThrow { CustomException(ErrorCode.USER_NOT_FOUND) }
    }
}
