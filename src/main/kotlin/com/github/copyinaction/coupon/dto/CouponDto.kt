package com.github.copyinaction.coupon.dto

import com.github.copyinaction.coupon.domain.Coupon
import com.github.copyinaction.coupon.domain.DiscountMethod
import com.github.copyinaction.coupon.domain.UserCoupon
import com.github.copyinaction.coupon.domain.UserCouponStatus
import java.time.LocalDateTime

data class CouponIssueRequest(
    val couponCode: String
)

data class CouponResponse(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
    val discountMethod: DiscountMethod,
    val discountValue: Int,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val minOrderAmount: Int?,
    val maxDiscountAmount: Int?
) {
    companion object {
        fun from(coupon: Coupon): CouponResponse {
            return CouponResponse(
                id = coupon.id,
                code = coupon.code,
                name = coupon.name,
                description = coupon.description,
                discountMethod = coupon.discountMethod,
                discountValue = coupon.discountValue,
                validFrom = coupon.validFrom,
                validUntil = coupon.validUntil,
                minOrderAmount = coupon.minOrderAmount,
                maxDiscountAmount = coupon.maxDiscountAmount
            )
        }
    }
}

data class UserCouponResponse(
    val id: Long,
    val coupon: CouponResponse,
    val status: UserCouponStatus,
    val issuedAt: LocalDateTime,
    val usedAt: LocalDateTime?
) {
    companion object {
        fun from(userCoupon: UserCoupon): UserCouponResponse {
            return UserCouponResponse(
                id = userCoupon.id,
                coupon = CouponResponse.from(userCoupon.coupon),
                status = userCoupon.status,
                issuedAt = userCoupon.issuedAt,
                usedAt = userCoupon.usedAt
            )
        }
    }
}

data class CouponValidateRequest(
    val couponCode: String,
    val orderAmount: Int
)
