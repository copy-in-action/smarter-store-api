package com.github.copyinaction.notice.domain

enum class NoticeCategory(val description: String) {
    BOOKING_NOTICE("예매 유의사항"),
    BANK_TRANSFER("무통장입금 시 주의사항"),
    TICKET_PICKUP("티켓수령안내"),
    MOBILE_TICKET("모바일티켓 안내"),
    REFUND("환불 안내"),
    CANCELLATION("취소 및 환불 유의사항")
}
