package com.github.copyinaction.seat.domain

/**
 * 좌석 위치 Value Object
 */
data class SeatPosition(
    val row: Int,
    val col: Int
)

/**
 * 좌석 선택 집합 Value Object
 * 좌석 변경 내역 계산 로직을 포함합니다.
 */
data class SeatSelection(
    val seats: Set<SeatPosition>
) {
    /**
     * 다른 좌석 선택과 비교하여 변경 사항을 계산합니다.
     */
    fun calculateChanges(newSelection: SeatSelection): SeatChangeResult {
        val kept = this.seats.intersect(newSelection.seats)
        val released = this.seats - newSelection.seats
        val added = newSelection.seats - this.seats
        return SeatChangeResult(kept, released, added)
    }
}

/**
 * 좌석 변경 결과 Value Object
 */
data class SeatChangeResult(
    val kept: Set<SeatPosition>,
    val released: Set<SeatPosition>,
    val added: Set<SeatPosition>
)
