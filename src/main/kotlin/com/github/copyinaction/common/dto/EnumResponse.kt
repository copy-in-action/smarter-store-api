package com.github.copyinaction.common.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Enum 응답")
data class EnumResponse(
    @Schema(description = "코드 (영문)", example = "VIP")
    val code: String,

    @Schema(description = "라벨 (한글)", example = "VIP석")
    val label: String
)
