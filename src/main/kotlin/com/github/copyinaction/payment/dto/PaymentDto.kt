package com.github.copyinaction.payment.dto

import com.github.copyinaction.discount.domain.DiscountType
import com.github.copyinaction.payment.domain.Payment
import com.github.copyinaction.payment.domain.PaymentMethod
import com.github.copyinaction.payment.domain.PaymentStatus
import java.time.LocalDateTime
import java.util.*

/**
 * 결제 생성 요청 DTO
 */
data class PaymentCreateRequest(
    val bookingId: UUID,
    val paymentMethod: PaymentMethod,
    
    // 금액 검증용 필드 추가
    val totalAmount: Int,
    val ticketAmount: Int,
    val bookingFee: Int,
    val originalPrice: Int, // 서버 검증용 원가

    // 할인 적용 상세 내역
    val discounts: List<AppliedDiscountDto> = emptyList()
)

data class AppliedDiscountDto(
    val type: DiscountType,
    val name: String,
    val amount: Int,
    val referenceId: String? = null
)

/**
 * 결제 완료 승인 요청 DTO (PG 결과 반영)
 */
data class PaymentCompleteRequest(
    val pgProvider: String,
    val pgTransactionId: String,
    val cardCompany: String? = null,
    val cardNumberMasked: String? = null,
    val installmentMonths: Int? = null
)

/**
 * 결제 취소 요청 DTO
 */
data class PaymentCancelRequest(
    val reason: String
)

/**
 * 결제 기본 응답 DTO
 */
data class PaymentResponse(
    val id: UUID,
    val bookingId: UUID,
    val paymentNumber: String,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val finalPrice: Int,
    val requestedAt: LocalDateTime,
    val completedAt: LocalDateTime?
) {
    companion object {
        fun from(payment: Payment): PaymentResponse {
            return PaymentResponse(
                id = payment.id,
                bookingId = payment.booking.id!!,
                paymentNumber = payment.paymentNumber,
                paymentMethod = payment.paymentMethod,
                paymentStatus = payment.paymentStatus,
                finalPrice = payment.finalPrice,
                requestedAt = payment.requestedAt,
                completedAt = payment.completedAt
            )
        }
    }
}

/**
 * 결제 상세 응답 DTO (항목 포함)
 */
data class PaymentDetailResponse(
    val payment: PaymentResponse,
    val items: List<PaymentItemResponse>,
    val pgInfo: PgInfoResponse?
)

data class PaymentItemResponse(
    val performanceTitle: String,
    val seatLabel: String,
    val finalPrice: Int
)

data class PgInfoResponse(
    val provider: String,
    val transactionId: String,
    val cardCompany: String?,
    val cardNumber: String?
)
