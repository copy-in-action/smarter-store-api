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

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() } // CSRF 보호 비활성화 (Stateless API)
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) } // 세션 사용 안함
            .authorizeHttpRequests {
                // 인증 없이 접근을 허용할 경로
                it.requestMatchers(
                    "/api/auth/**",
                    "/api/admin/auth/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/actuator/health",
                ).permitAll()
                it.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/venues", "/api/venues/**").permitAll()
                it.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/performances", "/api/performances/**").permitAll()
                // 그 외 모든 경로는 인증 필요
                .anyRequest().authenticated()
            }
            // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
            .addFilterBefore(JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
