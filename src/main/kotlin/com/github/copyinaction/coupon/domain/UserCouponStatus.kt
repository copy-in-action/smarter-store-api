package com.github.copyinaction.coupon.domain

enum class UserCouponStatus(val description: String) {
    AVAILABLE("사용 가능"),
    USED("사용 완료"),
    EXPIRED("만료됨")
}
