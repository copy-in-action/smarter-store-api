package com.github.copyinaction.booking.domain

import com.github.copyinaction.seat.domain.SeatChangeResult
import com.github.copyinaction.seat.domain.SeatPosition
import com.github.copyinaction.venue.domain.SeatGrade
import java.util.UUID

/**
 * 예매 관련 도메인 이벤트 인터페이스
 */
interface BookingEvent

/**
 * 예매 시작(점유 시작) 이벤트
 */
data class BookingStartedEvent(
    val bookingId: UUID,
    val scheduleId: Long,
    val userId: Long,
    val seatChanges: SeatChangeResult
) : BookingEvent

/**
 * 예매 확정 이벤트
 */
data class BookingConfirmedEvent(
    val bookingId: UUID,
    val scheduleId: Long,
    val seats: List<SeatPosition>,
    val seatGrade: SeatGrade
) : BookingEvent

/**
 * 예매 취소 이벤트
 */
data class BookingCancelledEvent(
    val bookingId: UUID,
    val scheduleId: Long,
    val userId: Long,
    val seats: List<SeatPosition>,
    val previousStatus: BookingStatus
) : BookingEvent
