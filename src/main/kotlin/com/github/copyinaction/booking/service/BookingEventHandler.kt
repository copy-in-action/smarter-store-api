package com.github.copyinaction.booking.service

import com.github.copyinaction.booking.domain.BookingCancelledEvent
import com.github.copyinaction.booking.domain.BookingConfirmedEvent
import com.github.copyinaction.booking.domain.BookingStartedEvent
import com.github.copyinaction.booking.domain.BookingStatus
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
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

        try {
            seatOccupationService.confirmSeats(event.scheduleId, event.seats, event.seatGrade, schedule)
        } catch (e: CustomException) {
            throw e
        } catch (e: Exception) {
            log.error("예매 확정 처리 실패 - bookingId: {}, scheduleId: {}", event.bookingId, event.scheduleId, e)
            throw CustomException(ErrorCode.BOOKING_CONFIRM_FAILED)
        }
    }

    @EventListener
    @Transactional
    fun handleBookingCancelled(event: BookingCancelledEvent) {
        log.debug("이벤트 핸들러 - 예매 취소 처리: bookingId={}, status={}", event.bookingId, event.previousStatus)
        
        when (event.previousStatus) {
            BookingStatus.PENDING -> {
                seatOccupationService.releaseUserPendingSeats(event.scheduleId, event.userId, event.seats)
            }
            BookingStatus.CONFIRMED -> {
                seatOccupationService.releaseReservedSeats(event.scheduleId, event.userId, event.seats)
            }
            else -> {
                log.warn("처리할 필요 없는 상태의 예매 취소 이벤트입니다: {}", event.previousStatus)
            }
        }
    }
}
