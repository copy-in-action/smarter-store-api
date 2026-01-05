package com.github.copyinaction.notice.domain

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "공지사항 카테고리 (SYSTEM: 시스템 점검, BOOKING_NOTICE: 예매 유의사항, EVENT: 이벤트)",
    enumAsRef = true
)
enum class NoticeCategory(val description: String) {
    SYSTEM("시스템 점검"),
    BOOKING_NOTICE("예매 유의사항"),
    EVENT("이벤트")
}
