package com.github.copyinaction.payment.dto

import com.github.copyinaction.discount.domain.DiscountType
import com.github.copyinaction.payment.domain.Payment
import com.github.copyinaction.payment.domain.PaymentMethod
import com.github.copyinaction.payment.domain.PaymentStatus
import com.github.copyinaction.venue.domain.SeatGrade
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.*

/**
 * 결제 생성 요청 DTO
 */
@Schema(description = "결제 생성 요청")
data class PaymentCreateRequest(
    @Schema(description = "예매 ID")
    val bookingId: UUID,
    @Schema(description = "결제 수단")
    val paymentMethod: PaymentMethod,
    
    // 금액 검증용 필드
    @Schema(description = "총 결제 금액 (검증용)", example = "100000")
    val totalAmount: Int,
    @Schema(description = "순수 티켓 금액", example = "90000")
    val ticketAmount: Int,
    @Schema(description = "예매 수수료", example = "1000")
    val bookingFee: Int,
    @Schema(description = "할인 전 원가", example = "110000")
    val originalPrice: Int, // 서버 검증용 원가

    // 약관 동의 여부
    @Schema(description = "결제 약관 동의 여부")
    val isAgreed: Boolean,

    // 할인 적용 상세 내역 (쿠폰, 포인트, 프로모션 등 통합)
    @Schema(description = "적용된 할인 목록")
    val discounts: List<AppliedDiscountDto> = emptyList()
)

@Schema(description = "적용된 할인 정보")
data class AppliedDiscountDto(
    @Schema(description = "할인 유형 (COUPON, POINT, PROMOTION)")
    val type: DiscountType,
    @Schema(description = "할인명", example = "신규 가입 쿠폰")
    val name: String,
    @Schema(description = "할인 금액", example = "5000")
    val amount: Int,
    @Schema(description = "적용 쿠폰 ID (쿠폰 할인인 경우)")
    val couponId: Long? = null,
    @Schema(description = "적용 좌석 ID (특정 좌석 할인인 경우)")
    val bookingSeatId: Long? = null // 특정 좌석(BookingSeat)에 적용된 할인인 경우
)

/**
 * 결제 완료 승인 요청 DTO (PG 결과 반영)
 */
@Schema(description = "결제 완료 승인 요청 (PG 결과)")
data class PaymentCompleteRequest(
    @Schema(description = "PG사 명", example = "TossPayments")
    val pgProvider: String,
    @Schema(description = "PG사 거래 ID", example = "ts_20230101...")
    val pgTransactionId: String,
    @Schema(description = "카드사 명", example = "Hyundai")
    val cardCompany: String? = null,
    @Schema(description = "마스킹된 카드 번호", example = "1234-****-****-5678")
    val cardNumberMasked: String? = null,
    @Schema(description = "할부 개월 수 (0: 일시불)", example = "0")
    val installmentMonths: Int? = null
)

/**
 * 결제 취소 요청 DTO
 */
@Schema(description = "결제 취소 요청")
data class PaymentCancelRequest(
    @Schema(description = "취소 사유", example = "단순 변심")
    val reason: String
)

/**
 * 결제 기본 응답 DTO
 */
@Schema(description = "결제 정보 응답")
data class PaymentResponse(
    @Schema(description = "결제 ID")
    val id: UUID,
    @Schema(description = "예매 ID")
    val bookingId: UUID,
    @Schema(description = "결제 번호 (주문 번호)", example = "ORD-20230101-0001")
    val paymentNumber: String,
    @Schema(description = "결제 수단")
    val paymentMethod: PaymentMethod,
    @Schema(description = "결제 상태")
    val paymentStatus: PaymentStatus,
    @Schema(description = "최종 결제 금액")
    val finalPrice: Int,
    @Schema(description = "결제 요청 일시")
    val requestedAt: LocalDateTime,
    @Schema(description = "결제 완료 일시")
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
@Schema(description = "결제 상세 정보 응답")
data class PaymentDetailResponse(
    @Schema(description = "결제 기본 정보")
    val payment: PaymentResponse,
    @Schema(description = "결제 항목 목록 (티켓 등)")
    val items: List<PaymentItemResponse>,
    @Schema(description = "PG 결제 정보")
    val pgInfo: PgInfoResponse?
)

@Schema(description = "결제 항목 정보")
data class PaymentItemResponse(
    @Schema(description = "공연명")
    val performanceTitle: String,
    @Schema(description = "좌석 등급")
    val seatGrade: SeatGrade,
    @Schema(description = "구역")
    val section: String,
    @Schema(description = "행 번호")
    val row: Int,
    @Schema(description = "열 번호")
    val col: Int,
    @Schema(description = "항목 최종 금액 (할인 적용 후)")
    val finalPrice: Int
)

@Schema(description = "PG 결제 정보")
data class PgInfoResponse(
    @Schema(description = "PG사")
    val provider: String,
    @Schema(description = "PG사 거래 ID")
    val transactionId: String,
    @Schema(description = "카드사 명")
    val cardCompany: String?,
    @Schema(description = "카드 번호 (마스킹됨)")
    val cardNumber: String?
)
