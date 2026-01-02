package com.github.copyinaction.booking.service

import com.github.copyinaction.booking.domain.BookingCancelledEvent
import com.github.copyinaction.booking.domain.BookingConfirmedEvent
import com.github.copyinaction.booking.domain.BookingStartedEvent
import com.github.copyinaction.performance.repository.PerformanceScheduleRepository
import com.github.copyinaction.seat.service.SeatOccupationService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BookingEventHandler(
    private val seatOccupationService: SeatOccupationService,
    private val performanceScheduleRepository: PerformanceScheduleRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener
    @Transactional
    fun handleBookingStarted(event: BookingStartedEvent) {
        log.debug("이벤트 핸들러 - 예매 시작 처리: bookingId={}", event.bookingId)
        val schedule = performanceScheduleRepository.findByIdOrNull(event.scheduleId) ?: return
        
        // 1. 좌석 상태 DB 반영
        seatOccupationService.processSeatChanges(schedule, event.userId, event.seatChanges)
        
        // 2. SSE 이벤트 발행
        seatOccupationService.publishSeatEvents(event.scheduleId, event.seatChanges)
    }

    @EventListener
    @Transactional
    fun handleBookingConfirmed(event: BookingConfirmedEvent) {
        log.debug("이벤트 핸들러 - 예매 확정 처리: bookingId={}", event.bookingId)
        val schedule = performanceScheduleRepository.findByIdOrNull(event.scheduleId) ?: return
        
        seatOccupationService.confirmSeats(event.scheduleId, event.seats, event.seatGrade, schedule)
    }

    @EventListener
    @Transactional
    fun handleBookingCancelled(event: BookingCancelledEvent) {
        log.debug("이벤트 핸들러 - 예매 취소 처리: bookingId={}", event.bookingId)
        
        seatOccupationService.releaseUserPendingSeats(event.scheduleId, event.userId, event.seats)
    }
}
