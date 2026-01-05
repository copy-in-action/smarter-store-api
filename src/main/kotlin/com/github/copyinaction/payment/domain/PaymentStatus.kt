package com.github.copyinaction.payment.domain

enum class PaymentStatus(val description: String) {
    PENDING("결제대기"),
    COMPLETED("결제완료"),
    FAILED("결제실패"),
    CANCELLED("결제취소"),
    REFUNDED("환불완료"),
    PARTIAL_REFUNDED("부분환불")
}
