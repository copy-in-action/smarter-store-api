package com.github.copyinaction.config

import com.github.copyinaction.config.jwt.JwtAuthenticationFilter
import com.github.copyinaction.config.jwt.JwtTokenProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // for @PreAuthorize
class SecurityConfig(
    private val jwtTokenProvider: JwtTokenProvider,
    @Value("\${cors.allowed-origins:}") private val allowedOrigins: String
) {
    companion object {
        // 모든 HTTP 메서드 허용 (인증 불필요)
        private val PUBLIC_URLS = arrayOf(
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/email-verification/request",
            "/api/auth/confirm-otp",
            "/api/auth/logout",
            "/api/admin/auth/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/health",
            "/actuator/prometheus",
            "/.well-known/**",
            "/admin/**",
        )

        // GET만 허용 (인증 불필요) - 조회용 공개 API
        private val PUBLIC_GET_URLS = arrayOf(
            "/api/venues/**",
            "/api/performances/**",
            "/api/enums/**",
            "/api/schedules/**",
            "/api/home/**",
            "/api/notices/**",
        )
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        if (allowedOrigins.isNotBlank()) {
            configuration.allowedOrigins = allowedOrigins.split(",").map { it.trim() }
        }
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    /**
     * Spring Security 필터 체인 설정
     *
     * 권한 체계:
     * - 인증(Authentication): "누구야?" → 여기서 관리 (permitAll / authenticated)
     * - 인가(Authorization): "뭘 할 수 있어?" → 컨트롤러 @PreAuthorize에서 관리 (ADMIN, USER)
     *
     * permitAll() 매칭 안 되면 → authenticated() → JWT 토큰 필요 (없으면 401)
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() } // Stateless API이므로 CSRF 비활성화
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                // 공개 API (인증 불필요)
                it.requestMatchers(*PUBLIC_URLS).permitAll()
                it.requestMatchers(org.springframework.http.HttpMethod.GET, *PUBLIC_GET_URLS).permitAll()
                // 그 외 모든 요청은 인증 필요
                it.anyRequest().authenticated()
            }
            .addFilterBefore(JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
