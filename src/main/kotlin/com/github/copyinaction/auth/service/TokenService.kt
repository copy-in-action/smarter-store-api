package com.github.copyinaction.auth.service

import com.github.copyinaction.auth.domain.User
import com.github.copyinaction.auth.dto.AuthTokenInfo
import com.github.copyinaction.auth.repository.UserRepository
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.config.jwt.JwtTokenProvider
import com.github.copyinaction.repository.RefreshTokenRepository
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 토큰 발급 및 갱신 담당 서비스
 */
@Service
class TokenService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenProvider: JwtTokenProvider
) {

    /**
     * 사용자에 대한 Access/Refresh 토큰 발급
     */
    fun issueTokens(user: User): AuthTokenInfo {
        val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
        val principal = CustomUserDetails(user.id, user.email, "", authorities)
        val authentication = UsernamePasswordAuthenticationToken(principal, null, authorities)

        val accessToken = jwtTokenProvider.createAccessToken(authentication)
        val refreshToken = user.issueRefreshToken(jwtTokenProvider)

        return AuthTokenInfo(
            accessToken = accessToken,
            refreshToken = refreshToken.token,
            accessTokenExpiresIn = jwtTokenProvider.getAccessTokenValidityInSeconds()
        )
    }

    /**
     * Refresh Token으로 새 토큰 발급
     */
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
}
