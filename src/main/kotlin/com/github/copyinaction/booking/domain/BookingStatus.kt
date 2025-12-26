package com.github.copyinaction.booking.domain

import org.hibernate.annotations.Comment
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "예매 상태 (PENDING: 결제 대기중, CONFIRMED: 예매 확정, CANCELLED: 예매 취소, EXPIRED: 시간 만료)")
@Comment("예매 상태")
enum class BookingStatus(val description: String) {
    @Comment("결제 대기중")
    PENDING("결제 대기중"),

    @Comment("예매 확정")
    CONFIRMED("예매 확정"),

    @Comment("예매 취소")
    CANCELLED("예매 취소"),

    @Comment("시간 만료")
    EXPIRED("시간 만료")
}
