package com.github.copyinaction.common.policy

object BookingPolicy {
    /**
     * 예매 점유 유효 시간 (분)
     * 예매 생성 및 좌석 점유 시 이 시간을 기준으로 만료 시각이 설정됩니다.
     */
    const val BOOKING_HOLD_MINUTES = 2L
}
