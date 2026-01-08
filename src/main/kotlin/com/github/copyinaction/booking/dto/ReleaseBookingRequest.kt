package com.github.copyinaction.booking.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.util.UUID

/**
 * 예매 해제 요청 (navigator.sendBeacon용 POST 엔드포인트)
 */
@Schema(description = "예매 해제 요청 (POST 방식)")
data class ReleaseBookingRequest(
    @field:NotNull(message = "bookingId는 필수입니다.")
    @Schema(description = "예매 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val bookingId: UUID
)
