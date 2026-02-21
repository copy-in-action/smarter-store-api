package com.github.copyinaction.booking.dto

import com.github.copyinaction.booking.domain.Booking
import com.github.copyinaction.booking.domain.BookingStatus
import com.github.copyinaction.payment.domain.Payment
import com.github.copyinaction.payment.dto.PaymentDetailResponse
import com.github.copyinaction.payment.dto.PaymentDiscountResponse
import com.github.copyinaction.payment.dto.PaymentItemResponse
import com.github.copyinaction.payment.dto.PaymentResponse
import com.github.copyinaction.payment.dto.PgInfoResponse
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "예매 상세 내역 응답")
data class BookingDetailResponse(
    @Schema(description = "예매 ID")
    val bookingId: UUID,

    @Schema(description = "예매 번호")
    val bookingNumber: String,

    @Schema(description = "공연 ID")
    val performanceId: Long,

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

    @Schema(description = "좌석 목록")
    val seats: List<BookingSeatResponse>,

    @Schema(description = "사용자 이메일 (관리자용)")
    val userEmail: String? = null,

    @Schema(description = "사용자 이름 (관리자용)")
    val username: String? = null,

    @Schema(description = "결제 상세 정보 (결제 완료 시)")
    val paymentDetail: PaymentDetailResponse? = null
) {
    companion object {
        fun from(booking: Booking, payment: Payment? = null, isAdmin: Boolean = false): BookingDetailResponse {
            val schedule = booking.schedule
            val performance = schedule.performance
            val user = booking.siteUser

            val paymentDetail = payment?.let { p ->
                val items = p.paymentItems.map {
                    PaymentItemResponse(
                        performanceId = it.performanceId,
                        performanceTitle = it.performanceTitle,
                        seatGrade = it.seatGrade,
                        section = it.section,
                        row = it.rowNum,
                        col = it.colNum,
                        unitPrice = it.unitPrice,
                        discountAmount = it.discountAmount,
                        finalPrice = it.finalPrice
                    )
                }
                val discounts = p.discounts.map {
                    PaymentDiscountResponse.from(it)
                }
                val pgInfo = p.pgProvider?.let {
                    PgInfoResponse(
                        provider = it,
                        transactionId = p.pgTransactionId ?: "",
                        cardCompany = p.cardCompany,
                        cardNumber = p.cardNumberMasked
                    )
                }
                PaymentDetailResponse(
                    payment = PaymentResponse.from(p),
                    items = items,
                    discounts = discounts,
                    pgInfo = pgInfo
                )
            }

            return BookingDetailResponse(
                bookingId = booking.id!!,
                bookingNumber = booking.bookingNumber,
                performanceId = performance.id,
                performanceTitle = performance.title,
                mainImageUrl = performance.mainImageUrl,
                showDateTime = schedule.showDateTime,
                status = booking.bookingStatus,
                totalPrice = booking.totalPrice,
                createdAt = booking.createdAt ?: LocalDateTime.now(),
                seats = booking.bookingSeats.map { BookingSeatResponse.from(it) },
                userEmail = if (isAdmin) user.email else null,
                username = if (isAdmin) user.username else null,
                paymentDetail = paymentDetail
            )
        }
    }
}
