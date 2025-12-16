package com.github.copyinaction.config.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = resolveToken(request)
        logger.debug("JwtAuthenticationFilter - URI: ${request.requestURI}, token exists: ${token != null}")

        if (token != null) {
            val isValid = jwtTokenProvider.validateToken(token)
            logger.debug("JwtAuthenticationFilter - token valid: $isValid")

            if (isValid) {
                val authentication = jwtTokenProvider.getAuthentication(token)
                SecurityContextHolder.getContext().authentication = authentication
                logger.debug("JwtAuthenticationFilter - authentication set: ${authentication.name}")
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        // 1. Authorization 헤더에서 Bearer 토큰 확인 (Swagger, Postman 등)
        val bearerToken = request.getHeader("Authorization")
        logger.info("resolveToken - Authorization header: ${bearerToken?.take(50) ?: "null"}")

        if (!bearerToken.isNullOrBlank() && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7)
        }

        // 2. 쿠키에서 accessToken 확인 (브라우저)
        val cookies = request.cookies
        val cookieNames = cookies?.map { it.name } ?: emptyList()
        logger.info("resolveToken - cookies: $cookieNames")

        if (cookies != null) {
            for (cookie in cookies) {
                if (cookie.name == "accessToken") {
                    return cookie.value
                }
            }
        }
        return null
    }
}
