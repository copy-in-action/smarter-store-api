package com.github.copyinaction.dto

import com.github.copyinaction.domain.Role
import com.github.copyinaction.domain.User
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.security.crypto.password.PasswordEncoder

@Schema(description = "로그인 요청 DTO")
data class LoginRequest(
    @field:NotBlank
    @field:Email
    @Schema(description = "이메일", example = "user@example.com")
    val email: String,

    @field:NotBlank
    @Schema(description = "비밀번호", example = "password123")
    val password: String
)

@Schema(description = "회원가입 요청 DTO")
data class SignupRequest(
    @field:NotBlank
    @field:Email
    @Schema(description = "이메일", example = "user@example.com")
    val email: String,

    @field:NotBlank
    @Schema(description = "비밀번호", example = "password123")
    val password: String
) {
    fun toEntity(passwordEncoder: PasswordEncoder): User {
        return User(
            email = this.email,
            passwordHash = passwordEncoder.encode(this.password),
            role = Role.USER // 기본 역할은 USER
        )
    }
}


@Schema(description = "JWT 토큰 응답 DTO")
data class TokenResponse(
    @Schema(description = "JWT Access Token")
    val accessToken: String
)
