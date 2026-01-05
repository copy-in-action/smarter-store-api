package com.github.copyinaction.payment.domain

enum class PaymentMethod(val description: String) {
    CREDIT_CARD("신용카드"),
    DEBIT_CARD("체크카드"),
    BANK_TRANSFER("계좌이체"),
    VIRTUAL_ACCOUNT("가상계좌"),
    KAKAO_PAY("카카오페이"),
    NAVER_PAY("네이버페이"),
    TOSS_PAY("토스페이"),
    PAYCO("페이코"),
    POINT("포인트")
}
