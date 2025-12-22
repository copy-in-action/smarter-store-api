package com.github.copyinaction.admin.service

import com.github.copyinaction.config.jwt.JwtTokenProvider
import com.github.copyinaction.admin.domain.Admin
import com.github.copyinaction.admin.dto.AdminLoginRequest
import com.github.copyinaction.admin.dto.AdminSignupRequest
import com.github.copyinaction.auth.service.CustomUserDetails
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.admin.repository.AdminRepository
import com.github.copyinaction.auth.dto.AuthTokenInfo
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminAuthService(
    private val adminRepository: AdminRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional
    fun signup(request: AdminSignupRequest): Admin {
        if (adminRepository.findByLoginId(request.loginId).isPresent) {
            throw CustomException(ErrorCode.ADMIN_LOGIN_ID_ALREADY_EXISTS)
        }

        // 도메인에서 Admin 생성
        val admin = Admin.create(
            loginId = request.loginId,
            name = request.name,
            rawPassword = request.password,
            passwordEncoder = passwordEncoder
        )
        return adminRepository.save(admin)
    }

    @Transactional(readOnly = true)
    fun login(request: AdminLoginRequest): AuthTokenInfo {
        val admin = adminRepository.findByLoginId(request.loginId)
            .orElseThrow { CustomException(ErrorCode.ADMIN_LOGIN_FAILED) }

        // 도메인에서 비밀번호 검증
        admin.validatePassword(request.password, passwordEncoder)

        val authorities = listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
        val principal = CustomUserDetails(admin.id, admin.loginId, "", authorities)
        val authentication = UsernamePasswordAuthenticationToken(principal, null, authorities)
        val accessToken = jwtTokenProvider.createAccessToken(authentication)

        return AuthTokenInfo(
            accessToken = accessToken,
            refreshToken = "",
            accessTokenExpiresIn = jwtTokenProvider.getAccessTokenValidityInSeconds()
        )
    }
}