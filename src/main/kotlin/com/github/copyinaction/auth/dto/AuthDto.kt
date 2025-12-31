package com.github.copyinaction.auth.dto

import com.github.copyinaction.auth.domain.User
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

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
    val password: String,

    @field:NotBlank
    @Schema(description = "핸드폰 번호", example = "01012345678")
    val phoneNumber: String
)

@Schema(description = "OTP 인증 확인 요청 DTO")
data class OtpConfirmationRequest(
    @field:NotBlank
    @field:Email
    @Schema(description = "사용자 이메일", example = "user@example.com")
    val email: String,

    @field:NotBlank
    @Schema(description = "6자리 OTP", example = "123456")
    val otp: String
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

@Schema(description = "로그인 응답 DTO")
data class LoginResponse(
    @Schema(description = "인증 토큰 정보")
    val token: AuthTokenInfo,
    @Schema(description = "사용자 정보")
    val user: UserResponse
)

@Schema(description = "사용자 정보 응답 DTO")
data class UserResponse(
    @Schema(description = "사용자 ID", example = "1")
    val id: Long,

    @Schema(description = "사용자 이메일", example = "user@example.com")
    val email: String,

    @Schema(description = "사용자 이름", example = "홍길동")
    val username: String,

    @Schema(description = "핸드폰 번호", example = "01012345678")
    val phoneNumber: String?,

    @Schema(description = "사용자 생성일시")
    val createdAt: java.time.LocalDateTime?
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id,
                email = user.email,
                username = user.username,
                phoneNumber = user.phoneNumber,
                createdAt = user.createdAt
            )
        }
    }
}
