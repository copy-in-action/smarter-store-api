package com.github.copyinaction.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.security.crypto.password.PasswordEncoder
import com.github.copyinaction.auth.domain.Role
import com.github.copyinaction.auth.domain.User

@Schema(description = "로그인 요청 DTO")
data class LoginRequest(
    @field:NotBlank
    @field:Email
    @Schema(description = "사용자 이메일", example = "user@example.com")
    val email: String,

    @field:NotBlank
    @Schema(description = "사용자 비밀번호", example = "password123")
    val password: String
)

@Schema(description = "회원가입 요청 DTO")
data class SignupRequest(
    @field:NotBlank
    @field:Email
    @Schema(description = "사용자 이메일", example = "user@example.com")
    val email: String,

    @field:NotBlank
    @Schema(description = "사용자 이름", example = "홍길동")
    val username: String,

    @field:NotBlank
    @Schema(description = "사용자 비밀번호", example = "password123")
    val password: String
) {
    fun toEntity(passwordEncoder: PasswordEncoder, role: Role = Role.USER): User {
        return User(
            email = this.email,
            username = this.username,
            passwordHash = passwordEncoder.encode(this.password),
            role = role
        )
    }
}




@Schema(description = "토큰 갱신 요청 DTO")
data class RefreshTokenRequest(
    @field:NotBlank
    @Schema(description = "리프레시 토큰", example = "2f0d2570-49ff-4343-96a6-ccba...")
    val refreshToken: String
)

@Schema(description = "인증 토큰 정보")
data class AuthTokenInfo(
    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzUxMiJ9...")
    val accessToken: String,
    @Schema(description = "리프레시 토큰", example = "2f0d2570-49ff-4343-96a6-ccba...")
    val refreshToken: String,
    @Schema(description = "액세스 토큰 만료 시간 (초)", example = "3600")
    val accessTokenExpiresIn: Long
)

@Schema(description = "사용자 정보 응답 DTO")
data class UserResponse(
    @Schema(description = "사용자 ID", example = "1")
    val id: Long,

    @Schema(description = "사용자 이메일", example = "user@example.com")
    val email: String,

    @Schema(description = "사용자 이름", example = "홍길동")
    val username: String,

    @Schema(description = "사용자 생성일시")
    val createdAt: java.time.LocalDateTime?
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id,
                email = user.email,
                username = user.username,
                createdAt = user.createdAt
            )
        }
    }
}
