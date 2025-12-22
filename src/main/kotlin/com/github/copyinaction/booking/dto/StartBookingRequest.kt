package com.github.copyinaction.booking.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "예매 시작 요청")
data class StartBookingRequest(
    @field:NotNull(message = "scheduleId는 필수입니다.")
    @Schema(description = "공연 회차 ID", example = "1")
    val scheduleId: Long
)
