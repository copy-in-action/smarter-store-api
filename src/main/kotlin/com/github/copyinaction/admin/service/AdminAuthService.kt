package com.github.copyinaction.admin.service

import com.github.copyinaction.config.jwt.JwtTokenProvider
import com.github.copyinaction.admin.domain.Admin
import com.github.copyinaction.admin.dto.AdminLoginRequest
import com.github.copyinaction.admin.dto.AdminSignupRequest
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
        val admin = request.toEntity(passwordEncoder)
        return adminRepository.save(admin)
    }

    @Transactional(readOnly = true)
    fun login(request: AdminLoginRequest): AuthTokenInfo {
        val admin = adminRepository.findByLoginId(request.loginId)
            .orElseThrow { CustomException(ErrorCode.ADMIN_LOGIN_FAILED) }

        if (!passwordEncoder.matches(request.password, admin.passwordHash)) {
            throw CustomException(ErrorCode.ADMIN_LOGIN_FAILED)
        }

        val authorities = listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
        val authentication = UsernamePasswordAuthenticationToken(admin.loginId, null, authorities)
        val accessToken = jwtTokenProvider.createAccessToken(authentication)

        return AuthTokenInfo(
            accessToken = accessToken,
            refreshToken = "", // 관리자는 refresh token 미사용 (빈 문자열)
            accessTokenExpiresIn = jwtTokenProvider.getAccessTokenValidityInSeconds()
        )
    }
}