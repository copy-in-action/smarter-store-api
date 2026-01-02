package com.github.copyinaction.booking.dto

import com.github.copyinaction.booking.domain.Booking
import com.github.copyinaction.booking.domain.BookingStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "예매 상세 응답")
data class BookingResponse(
    @Schema(description = "예매 ID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    val bookingId: UUID,

    @Schema(description = "예매 번호", example = "20251222-ABCDEFGH")
    val bookingNumber: String,

    @Schema(description = "만료 시각 (ISO 8601)", example = "2025-12-22T15:30:00")
    val expiresAt: LocalDateTime,

    @Schema(description = "남은 시간 (초)", example = "120")
    val remainingSeconds: Long,

    @Schema(description = "예매 좌석 목록")
    val seats: List<BookingSeatResponse>,

    @Schema(description = "총 결제 금액", example = "150000")
    val totalPrice: Int,

    @Schema(description = "예매 상태", example = "PENDING")
    val status: BookingStatus
) {
    companion object {
        fun from(booking: Booking): BookingResponse {
            return BookingResponse(
                bookingId = booking.id!!,
                bookingNumber = booking.bookingNumber,
                expiresAt = booking.expiresAt,
                remainingSeconds = booking.getRemainingSeconds(),
                seats = booking.bookingSeats.map { BookingSeatResponse.from(it) },
                totalPrice = booking.totalPrice,
                status = booking.bookingStatus
            )
        }
    }
}
