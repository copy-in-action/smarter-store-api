package com.github.copyinaction.service

import com.github.copyinaction.config.jwt.JwtTokenProvider
import com.github.copyinaction.domain.RefreshToken
import com.github.copyinaction.domain.User
import com.github.copyinaction.dto.LoginRequest
import com.github.copyinaction.dto.SignupRequest
import com.github.copyinaction.dto.TokenResponse
import com.github.copyinaction.exception.CustomException
import com.github.copyinaction.exception.ErrorCode
import com.github.copyinaction.repository.RefreshTokenRepository
import com.github.copyinaction.repository.UserRepository
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
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManagerBuilder: AuthenticationManagerBuilder,
    private val jwtTokenProvider: JwtTokenProvider
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
    fun login(request: LoginRequest): TokenResponse {
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

        return TokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken.token,
            expiresIn = jwtTokenProvider.getAccessTokenValidityInSeconds()
        )
    }

    @Transactional
    fun refresh(refreshTokenString: String): TokenResponse {
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

        return TokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken.token,
            expiresIn = jwtTokenProvider.getAccessTokenValidityInSeconds()
        )
    }
}
