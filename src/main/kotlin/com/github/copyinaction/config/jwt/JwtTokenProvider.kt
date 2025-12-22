package com.github.copyinaction.config.jwt

import com.github.copyinaction.auth.service.CustomUserDetails
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.Base64
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secretString: String,
    @Value("\${jwt.access-token-validity-in-seconds}") private val accessTokenValidityInSeconds: Long,
    @Value("\${jwt.refresh-token-validity-in-seconds}") private val refreshTokenValidityInSeconds: Long,
) {
    private val secretKey: SecretKey by lazy {
        // Base64로 인코딩된 비밀 키를 디코딩합니다.
        // JWT HS256은 최소 256비트(32바이트) 키를 요구합니다.
        Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretString))
    }

    /**
     * 인증 정보를 기반으로 JWT Access Token 생성
     */
    fun createAccessToken(authentication: Authentication): String {
        val principal = authentication.principal as CustomUserDetails
        val authorities = authentication.authorities.joinToString(",") { it.authority }
        val now = Date()
        val validity = Date(now.time + accessTokenValidityInSeconds * 1000)

        return Jwts.builder()
            .subject(principal.username)
            .claim("userId", principal.id)
            .claim("auth", authorities)
            .signWith(secretKey)
            .issuedAt(now)
            .expiration(validity)
            .compact()
    }

    /**
     * JWT 토큰에서 인증 정보 조회
     */
    fun getAuthentication(token: String): Authentication {
        val claims: Claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

        val authorities = claims["auth"].toString().split(",").map(::SimpleGrantedAuthority)
        val userId = (claims["userId"] as? Number)?.toLong()
            ?: throw RuntimeException("userId 클레임을 찾을 수 없습니다. 재로그인이 필요합니다.")

        // @AuthenticationPrincipal CustomUserDetails가 동작하도록 CustomUserDetails 객체 생성
        val principal = CustomUserDetails(userId, claims.subject, "", authorities)

        return UsernamePasswordAuthenticationToken(principal, token, authorities)
    }

    /**
     * JWT 토큰의 유효성 검증
     */
    fun validateToken(token: String): Boolean {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token)
            return true
        } catch (e: Exception) {
            // TODO: 로그 남기기 (JwtException, SecurityException, MalformedJwtException, ExpiredJwtException, etc.)
        }
        return false
    }

    /**
     * Refresh Token 문자열 생성 (UUID 기반)
     */
    fun createRefreshToken(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * Refresh Token 만료일시 계산
     */
    fun getRefreshTokenExpiryDate(): LocalDateTime {
        return LocalDateTime.now().plusSeconds(refreshTokenValidityInSeconds)
    }

    /**
     * Access Token 유효시간(초) 반환
     */
    fun getAccessTokenValidityInSeconds(): Long {
        return accessTokenValidityInSeconds
    }
}
