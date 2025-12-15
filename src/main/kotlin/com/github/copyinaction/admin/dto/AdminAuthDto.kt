package com.github.copyinaction.admin.dto

import com.github.copyinaction.admin.domain.Admin
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "관리자 로그인 요청 DTO")
data class AdminLoginRequest(
    @field:NotBlank
    @Schema(description = "관리자 로그인 ID", example = "admin")
    val loginId: String,

    @field:NotBlank
    @Schema(description = "관리자 비밀번호", example = "password123")
    val password: String
)

@Schema(description = "관리자 회원가입 요청 DTO")
data class AdminSignupRequest(
    @field:NotBlank
    @Schema(description = "관리자 로그인 ID", example = "admin")
    val loginId: String,

    @field:NotBlank
    @Schema(description = "관리자 이름", example = "관리자")
    val name: String,

    @field:NotBlank
    @Schema(description = "관리자 비밀번호", example = "password123")
    val password: String
)

@Schema(description = "관리자 정보 응답 DTO")
data class AdminResponse(
    @Schema(description = "관리자 ID", example = "1")
    val id: Long,

    @Schema(description = "관리자 로그인 ID", example = "admin")
    val loginId: String,

    @Schema(description = "관리자 이름", example = "관리자")
    val name: String,

    @Schema(description = "관리자 생성일시")
    val createdAt: java.time.LocalDateTime?
) {
    companion object {
        fun from(admin: Admin): AdminResponse {
            return AdminResponse(
                id = admin.id,
                loginId = admin.loginId,
                name = admin.name,
                createdAt = admin.createdAt
            )
        }
    }
}
