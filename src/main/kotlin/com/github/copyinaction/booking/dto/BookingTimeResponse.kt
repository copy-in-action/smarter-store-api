package com.github.copyinaction.booking.dto

import com.github.copyinaction.booking.domain.Booking
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "예매 시간 정보 응답")
data class BookingTimeResponse(
    @Schema(description = "예매 ID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    val bookingId: UUID,

    @Schema(description = "남은 시간 (초)", example = "120")
    val remainingSeconds: Long,

    @Schema(description = "예매 만료 여부", example = "false")
    val expired: Boolean
) {
    companion object {
        fun from(booking: Booking): BookingTimeResponse {
            val remainingSeconds = booking.getRemainingSeconds()
            return BookingTimeResponse(
                bookingId = booking.id,
                remainingSeconds = remainingSeconds,
                expired = remainingSeconds <= 0
            )
        }
    }
}
