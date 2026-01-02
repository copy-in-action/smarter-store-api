package com.github.copyinaction.venue.domain

/**
 * 좌석 배치도 변경 이벤트
 */
data class SeatingChartUpdatedEvent(
    val venueId: Long,
    val seatingChartJson: String
)
