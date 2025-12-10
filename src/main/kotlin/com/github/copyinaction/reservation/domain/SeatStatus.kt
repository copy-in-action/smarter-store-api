package com.github.copyinaction.reservation.domain

enum class SeatStatus {
    AVAILABLE, // 사용 가능
    HELD,      // 임시 점유
    RESERVED,  // 예매 완료
    UNAVAILABLE // 사용 불가
}
