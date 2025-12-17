package com.github.copyinaction.seat.domain

/**
 * 좌석 상태
 */
enum class SeatStatus {
    /**
     * 점유 중 (결제 진행 중, 10분 제한)
     */
    PENDING,

    /**
     * 예약 완료 (결제 완료)
     */
    RESERVED
}
