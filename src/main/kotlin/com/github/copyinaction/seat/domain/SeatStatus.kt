package com.github.copyinaction.seat.domain

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "좌석 점유 상태 (PENDING: 점유 중, RESERVED: 예약 완료)",
    enumAsRef = true
)
enum class SeatStatus(val description: String) {
    /**
     * 점유 중 (결제 진행 중, 10분 제한)
     */
    PENDING("점유 중"),

    /**
     * 예약 완료 (결제 완료)
     */
    RESERVED("예약 완료")
}
