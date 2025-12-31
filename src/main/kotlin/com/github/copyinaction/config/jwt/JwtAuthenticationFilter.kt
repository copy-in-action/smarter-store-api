package com.github.copyinaction.config.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        resolveToken(request)?.let { token ->
            if (jwtTokenProvider.validateToken(token)) {
                try {
                    val authentication = jwtTokenProvider.getAuthentication(token)
                    SecurityContextHolder.getContext().authentication = authentication
                } catch (e: Exception) {
                    logger.warn("인증 처리 실패 - 재로그인 필요: ${e.message}")
                }
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        // 1. Authorization 헤더에서 Bearer 토큰 확인 (Swagger, Postman 등)
        val bearerToken = request.getHeader("Authorization")
        if (!bearerToken.isNullOrBlank() && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7)
        }

        // 2. 쿠키에서 accessToken 확인 (브라우저)
        return request.cookies?.find { it.name == "accessToken" }?.value
    }
}
