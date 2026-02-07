package com.github.copyinaction.booking.dto

import com.github.copyinaction.booking.domain.Booking
import com.github.copyinaction.booking.domain.BookingStatus
import com.github.copyinaction.payment.domain.Payment
import com.github.copyinaction.payment.domain.PaymentStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "관리자용 예매 내역 상세 응답")
data class AdminBookingResponse(
    @Schema(description = "예매 ID")
    val bookingId: UUID,

    @Schema(description = "예매 번호")
    val bookingNumber: String,

    @Schema(description = "예매자 이메일")
    val userEmail: String,

    @Schema(description = "예매자 이름")
    val username: String,

    @Schema(description = "예매자 연락처")
    val phoneNumber: String?,

    @Schema(description = "예매 상태")
    val status: BookingStatus,

    @Schema(description = "총 결제 금액")
    val totalPrice: Int,

    @Schema(description = "예매 일시")
    val createdAt: LocalDateTime,

    @Schema(description = "좌석 정보 요약")
    val seatSummary: String,

    @Schema(description = "결제 상태")
    val paymentStatus: PaymentStatus?,

    @Schema(description = "결제 수단")
    val paymentMethod: String?,

    @Schema(description = "결제/환불 완료 시각")
    val paymentCompletedAt: LocalDateTime?
) {
    companion object {
        fun from(booking: Booking, payment: Payment?): AdminBookingResponse {
            val user = booking.siteUser
            val seatSummary = booking.bookingSeats.joinToString(", ") { 
                "${it.section}구역 ${it.row}행 ${it.col}열"
            }

            return AdminBookingResponse(
                bookingId = booking.id!!,
                bookingNumber = booking.bookingNumber,
                userEmail = user.email,
                username = user.username,
                phoneNumber = user.phoneNumber,
                status = booking.bookingStatus,
                totalPrice = booking.totalPrice,
                createdAt = booking.createdAt ?: LocalDateTime.now(),
                seatSummary = seatSummary,
                paymentStatus = payment?.paymentStatus,
                paymentMethod = payment?.paymentMethod?.name,
                paymentCompletedAt = payment?.completedAt ?: payment?.refundedAt
            )
        }
    }
}
