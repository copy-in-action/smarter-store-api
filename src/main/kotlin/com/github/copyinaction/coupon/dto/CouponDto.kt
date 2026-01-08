package com.github.copyinaction.coupon.dto

import com.github.copyinaction.coupon.domain.Coupon
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime
import java.util.UUID

// ==================== 관리자용 DTO ====================

@Schema(description = "쿠폰 생성 요청 (관리자)")
data class CouponCreateRequest(
    @field:NotBlank(message = "쿠폰명은 필수입니다")
    @Schema(description = "쿠폰명", example = "신년 맞이 할인 쿠폰")
    val name: String,

    @field:Min(value = 1, message = "할인율은 1 이상이어야 합니다")
    @Schema(description = "할인율 (%)", example = "10")
    val discountRate: Int,

    @field:NotNull(message = "유효 시작일은 필수입니다")
    @Schema(description = "유효 시작일")
    val validFrom: LocalDateTime,

    @field:NotNull(message = "유효 종료일은 필수입니다")
    @Schema(description = "유효 종료일")
    val validUntil: LocalDateTime
)

@Schema(description = "쿠폰 수정 요청 (관리자)")
data class CouponUpdateRequest(
    @field:NotBlank(message = "쿠폰명은 필수입니다")
    @Schema(description = "쿠폰명", example = "신년 맞이 할인 쿠폰")
    val name: String,

    @field:Min(value = 1, message = "할인율은 1 이상이어야 합니다")
    @Schema(description = "할인율 (%)", example = "10")
    val discountRate: Int,

    @field:NotNull(message = "유효 시작일은 필수입니다")
    @Schema(description = "유효 시작일")
    val validFrom: LocalDateTime,

    @field:NotNull(message = "유효 종료일은 필수입니다")
    @Schema(description = "유효 종료일")
    val validUntil: LocalDateTime,

    @field:NotNull(message = "활성화 여부는 필수입니다")
    @Schema(description = "활성화 여부", example = "true")
    val isActive: Boolean
)

@Schema(description = "쿠폰 응답 (관리자)")
data class CouponResponse(
    @Schema(description = "쿠폰 ID")
    val id: Long,
    @Schema(description = "쿠폰명")
    val name: String,
    @Schema(description = "할인율")
    val discountRate: Int,
    @Schema(description = "유효 시작일")
    val validFrom: LocalDateTime,
    @Schema(description = "유효 종료일")
    val validUntil: LocalDateTime,
    @Schema(description = "활성화 여부")
    val isActive: Boolean
) {
    companion object {
        fun from(coupon: Coupon): CouponResponse {
            return CouponResponse(
                id = coupon.id,
                name = coupon.name,
                discountRate = coupon.discountRate,
                validFrom = coupon.validFrom,
                validUntil = coupon.validUntil,
                isActive = coupon.isActive
            )
        }
    }
}

// ==================== 사용자용 DTO ====================

@Schema(description = "사용 가능한 쿠폰 응답")
data class AvailableCouponResponse(
    @Schema(description = "쿠폰 ID")
    val id: Long,
    @Schema(description = "쿠폰명")
    val name: String,
    @Schema(description = "할인율")
    val discountRate: Int,
    @Schema(description = "유효 종료일")
    val validUntil: LocalDateTime
) {
    companion object {
        fun from(coupon: Coupon): AvailableCouponResponse {
            return AvailableCouponResponse(
                id = coupon.id,
                name = coupon.name,
                discountRate = coupon.discountRate,
                validUntil = coupon.validUntil
            )
        }
    }
}

// ==================== 좌석별 쿠폰 적용 DTO ====================

@Schema(description = "좌석별 쿠폰 적용 요청")
data class SeatCouponRequest(
    @field:NotNull(message = "예매 좌석 ID는 필수입니다")
    @Schema(description = "예매 좌석 ID (BookingSeat ID)", example = "1")
    val bookingSeatId: Long,

    @field:NotNull(message = "쿠폰 ID는 필수입니다")
    @Schema(description = "적용할 쿠폰 ID", example = "1")
    val couponId: Long,

    @field:Min(value = 1, message = "원가는 1 이상이어야 합니다")
    @Schema(description = "좌석 원가", example = "50000")
    val originalPrice: Int
)

@Schema(description = "쿠폰 검증 요청")
data class CouponValidateRequest(
    @field:NotNull(message = "예매 ID는 필수입니다")
    @Schema(description = "예매 ID")
    val bookingId: java.util.UUID,

    @field:Valid
    @Schema(description = "좌석별 쿠폰 적용 목록")
    val seatCoupons: List<SeatCouponRequest> = emptyList()
)

@Schema(description = "좌석별 쿠폰 적용 결과")
data class SeatCouponResult(
    @Schema(description = "예매 좌석 ID")
    val bookingSeatId: Long,
    @Schema(description = "쿠폰 ID")
    val couponId: Long,
    @Schema(description = "원가")
    val originalPrice: Int,
    @Schema(description = "할인금액")
    val discountAmount: Int,
    @Schema(description = "최종가격")
    val finalPrice: Int,
    @Schema(description = "적용 가능 여부")
    val isValid: Boolean,
    @Schema(description = "메시지 (오류 시)")
    val message: String? = null
)

@Schema(description = "쿠폰 검증 응답")
data class CouponValidateResponse(
    @Schema(description = "좌석별 적용 결과")
    val results: List<SeatCouponResult>,
    @Schema(description = "총 원가")
    val totalOriginalPrice: Int,
    @Schema(description = "총 할인금액")
    val totalDiscountAmount: Int,
    @Schema(description = "총 최종가격")
    val totalFinalPrice: Int,
    @Schema(description = "전체 검증 성공 여부")
    val allValid: Boolean
)
