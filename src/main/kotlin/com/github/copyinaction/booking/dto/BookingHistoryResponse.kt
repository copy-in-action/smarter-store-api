package com.github.copyinaction.booking.dto

import com.github.copyinaction.booking.domain.Booking
import com.github.copyinaction.booking.domain.BookingStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "예매 내역 응답 (마이페이지용)")
data class BookingHistoryResponse(
    @Schema(description = "예매 ID")
    val bookingId: UUID,

    @Schema(description = "예매 번호")
    val bookingNumber: String,

    @Schema(description = "공연 제목")
    val performanceTitle: String,

    @Schema(description = "공연 메인 이미지 URL")
    val mainImageUrl: String?,

    @Schema(description = "공연 일시")
    val showDateTime: LocalDateTime,

    @Schema(description = "예매 상태")
    val status: BookingStatus,

    @Schema(description = "총 결제 금액")
    val totalPrice: Int,

    @Schema(description = "예매 일시")
    val createdAt: LocalDateTime,

    @Schema(description = "좌석 정보 요약 (예: A구역 1행 1열 등)")
    val seatSummary: String
) {
    companion object {
        fun from(booking: Booking): BookingHistoryResponse {
            val schedule = booking.schedule
            val performance = schedule.performance
            
            val seatSummary = booking.bookingSeats.joinToString(", ") { 
                "${it.section}구역 ${it.row}행 ${it.col}열"
            }

            return BookingHistoryResponse(
                bookingId = booking.id!!,
                bookingNumber = booking.bookingNumber,
                performanceTitle = performance.title,
                mainImageUrl = performance.mainImageUrl,
                showDateTime = schedule.showDateTime,
                status = booking.bookingStatus,
                totalPrice = booking.totalPrice,
                createdAt = booking.createdAt ?: LocalDateTime.now(),
                seatSummary = seatSummary
            )
        }
    }
}
