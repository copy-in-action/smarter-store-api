package com.github.copyinaction.booking.domain

import org.hibernate.annotations.Comment

@Comment("예매 상태")
enum class BookingStatus {
    @Comment("진행 중 (좌석 선택 ~ 결제 대기)")
    PENDING,

    @Comment("결제 완료")
    CONFIRMED,

    @Comment("사용자 취소")
    CANCELLED,

    @Comment("시간 만료")
    EXPIRED
}
