package com.github.copyinaction.dto

import com.github.copyinaction.domain.Admin
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.springframework.security.crypto.password.PasswordEncoder

@Schema(description = "관리자 로그인 요청 DTO")
data class AdminLoginRequest(
    @field:NotBlank
    @Schema(description = "로그인 ID", example = "admin")
    val loginId: String,

    @field:NotBlank
    @Schema(description = "비밀번호", example = "password123")
    val password: String
)

@Schema(description = "관리자 회원가입 요청 DTO")
data class AdminSignupRequest(
    @field:NotBlank
    @Schema(description = "로그인 ID", example = "admin")
    val loginId: String,

    @field:NotBlank
    @Schema(description = "이름", example = "관리자")
    val name: String,

    @field:NotBlank
    @Schema(description = "비밀번호", example = "password123")
    val password: String
) {
    fun toEntity(passwordEncoder: PasswordEncoder): Admin {
        return Admin(
            loginId = this.loginId,
            name = this.name,
            passwordHash = passwordEncoder.encode(this.password)
        )
    }
}

@Schema(description = "관리자 정보 응답 DTO")
data class AdminResponse(
    @Schema(description = "관리자 ID", example = "1")
    val id: Long,

    @Schema(description = "로그인 ID", example = "admin")
    val loginId: String,

    @Schema(description = "이름", example = "관리자")
    val name: String,

    @Schema(description = "생성일시")
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
