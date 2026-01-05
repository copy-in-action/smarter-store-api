package com.github.copyinaction.discount.domain

enum class DiscountType(val description: String) {
    COUPON("쿠폰 할인"),
    PROMOTION("프로모션 할인"), // 얼리버드 등
    GRADE("등급 할인"),
    POINT("포인트 사용"),
    WELFARE("복지 할인"), // 국가유공자, 장애인 등
    ETC("기타 할인")
}
