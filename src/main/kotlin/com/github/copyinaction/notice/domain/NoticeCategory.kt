package com.github.copyinaction.notice.domain

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = """
        공지사항 카테고리
        - BOOKING_NOTICE: 예매 유의사항
        - BANK_TRANSFER_NOTICE: 무통장입금 입금 시 주의사항
        - TICKET_RECEIPT_GUIDE: 티켓 수령안내
        - MOBILE_TICKET_GUIDE: 모바일 티켓 안내
        - REFUND_GUIDE: 환불 안내
        - CANCELLATION_REFUND_NOTICE: 취소 및 환불 유의사항
    """,
    enumAsRef = true
)
enum class NoticeCategory(val description: String) {
    BOOKING_NOTICE("예매 유의사항"),
    BANK_TRANSFER_NOTICE("무통장입금 입금 시 주의사항"),
    TICKET_RECEIPT_GUIDE("티켓 수령안내"),
    MOBILE_TICKET_GUIDE("모바일 티켓 안내"),
    REFUND_GUIDE("환불 안내"),
    CANCELLATION_REFUND_NOTICE("취소 및 환불 유의사항")
}
