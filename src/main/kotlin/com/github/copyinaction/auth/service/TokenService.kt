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
    @Transactional
    fun issueTokens(userParam: User): AuthTokenInfo {
        // 비관적 락을 사용하여 User 재조회 (동시 로그인 처리)
        val user = userRepository.findByIdWithLock(userParam.id)
            .orElseThrow { CustomException(ErrorCode.USER_NOT_FOUND) }

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

    /**
     * 로그아웃 시 사용자의 모든 Refresh Token 삭제
     */
    @Transactional
    fun revokeAllTokens(userId: Long) {
        refreshTokenRepository.deleteByUserId(userId)
    }

    /**
     * Refresh Token으로 새 토큰 발급
     */
    @Transactional
    fun refresh(refreshTokenString: String): AuthTokenInfo {
        // 1. 토큰 문자열로 기본 조회 (락 없음)
        val tempToken = refreshTokenRepository.findByToken(refreshTokenString)
            .orElseThrow { CustomException(ErrorCode.INVALID_REFRESH_TOKEN) }

        // 2. User ID로 비관적 락을 걸고 User 조회
        val user = userRepository.findByIdWithLock(tempToken.user.id)
            .orElseThrow { CustomException(ErrorCode.USER_NOT_FOUND) }

        // 3. 락이 걸린 상태에서 RefreshToken이 여전히 유효한지(User가 가지고 있는지) 검증
        // orphanRemoval=true로 인해 컬렉션에서 제거되면 DB에서도 삭제되므로,
        // user.refreshTokens 컬렉션 내에서 해당 토큰을 찾아야 안전함.
        val refreshToken = user.refreshTokens.find { it.token == refreshTokenString }
            ?: throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)

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
