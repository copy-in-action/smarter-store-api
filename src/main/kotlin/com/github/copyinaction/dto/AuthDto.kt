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
    @Schema(description = "사용자이름", example = "홍길동")
    val username: String,

    @field:NotBlank
    @Schema(description = "비밀번호", example = "password123")
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
    @Schema(description = "Refresh Token")
    val refreshToken: String
)

data class AuthTokenInfo(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresIn: Long
)

@Schema(description = "사용자 정보 응답 DTO")
data class UserResponse(
    @Schema(description = "사용자 ID", example = "1")
    val id: Long,

    @Schema(description = "이메일", example = "user@example.com")
    val email: String,

    @Schema(description = "사용자이름", example = "홍길동")
    val username: String,

    @Schema(description = "생성일시")
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
