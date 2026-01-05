package com.github.copyinaction.auth.domain

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "사용자 역할 (USER: 일반 사용자, ADMIN: 관리자)",
    enumAsRef = true
)
enum class Role(val description: String) {
    USER("일반 사용자"),
    ADMIN("관리자")
}
