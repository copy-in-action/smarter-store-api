package com.github.copyinaction.auth.dto

import jakarta.validation.constraints.NotBlank

data class EmailVerificationConfirm(
    @field:NotBlank(message = "인증 토큰은 필수입니다.")
    val token: String
)
