package com.github.copyinaction.booking.service

import com.github.copyinaction.auth.repository.UserRepository
import com.github.copyinaction.booking.domain.*
import com.github.copyinaction.booking.dto.BookingResponse
import com.github.copyinaction.booking.dto.SeatPositionRequest
import com.github.copyinaction.booking.repository.BookingRepository
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.payment.domain.PaymentStatus
import com.github.copyinaction.payment.repository.PaymentRepository
import com.github.copyinaction.payment.service.PaymentFacade
import com.github.copyinaction.performance.repository.PerformanceScheduleRepository
import com.github.copyinaction.seat.domain.SeatPosition
import com.github.copyinaction.seat.domain.SeatSelection
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * 예매 프로세스의 복잡한 오케스트레이션을 담당하는 Facade 클래스입니다.
 * BookingService(예매 도메인), PaymentFacade(결제 오케스트레이션), EventPublisher 간의 상호작용을 조율합니다.
 */
@Component
class BookingFacade(
    private val bookingService: BookingService,
    private val userRepository: UserRepository,
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val bookingRepository: BookingRepository,
    private val paymentRepository: PaymentRepository,
    private val paymentFacade: PaymentFacade,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun startBooking(scheduleId: Long, seats: List<SeatPositionRequest>, userId: Long): BookingResponse {
        val user = userRepository.findByIdOrNull(userId) ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        val schedule = performanceScheduleRepository.findByIdOrNull(scheduleId)
            ?: throw CustomException(ErrorCode.PERFORMANCE_SCHEDULE_NOT_FOUND)

        val newSeatSet = seats.map { SeatPosition(it.row, it.col) }.toSet()

        // 1. 기존 PENDING 상태의 예매 확인 및 취소 (오케스트레이션)
        val existingBooking = bookingRepository.findBySiteUser_IdAndSchedule_IdAndBookingStatus(userId, scheduleId, BookingStatus.PENDING)
        val oldSeatSet = if (existingBooking != null) {
            existingBooking.bookingSeats.map { SeatPosition(it.row, it.col) }.toSet()
        } else {
            emptySet()
        }

        if (existingBooking != null) {
            bookingService.releaseBookingInternal(existingBooking)
        }

        // 2. 좌석 변경 사항 계산 (비즈니스 로직)
        val oldSelection = SeatSelection(oldSeatSet)
        val newSelection = SeatSelection(newSeatSet)
        val seatChanges = oldSelection.calculateChanges(newSelection)

        // 3. Booking 및 BookingSeat 생성 (BookingService 위임)
        val savedBooking = bookingService.createAndSaveBooking(user, schedule, seats)

        // 4. 외부 이벤트 발행 (좌석 점유 시작)
        eventPublisher.publishEvent(BookingStartedEvent(
            bookingId = savedBooking.id!!,
            scheduleId = scheduleId,
            userId = userId,
            seatChanges = seatChanges
        ))

        log.info("예매 시작 - bookingId: {}, scheduleId: {}, 유지: {}, 추가: {}, 해제: {}",
            savedBooking.id, scheduleId, seatChanges.kept.size, seatChanges.added.size, seatChanges.released.size)

        return BookingResponse.from(savedBooking)
    }

    @Transactional
    fun confirmBooking(bookingId: UUID, userId: Long): BookingResponse {
        val booking = bookingRepository.findByIdOrNull(bookingId) ?: throw CustomException(ErrorCode.BOOKING_NOT_FOUND)
        bookingService.validateBookingOwner(booking, userId)

        if (booking.isExpired()) {
            bookingService.expireBookingInternal(booking)
            throw CustomException(ErrorCode.BOOKING_EXPIRED)
        }

        // 1. 예매 상태 변경 (BookingService 위임)
        bookingService.confirmBookingInternal(booking)

        // 2. 외부 이벤트 발행 (좌석 확정)
        val seatPositions = booking.bookingSeats.map { SeatPosition(it.row, it.col) }
        val seatGrade = booking.bookingSeats.firstOrNull()?.grade ?: com.github.copyinaction.venue.domain.SeatGrade.R

        eventPublisher.publishEvent(BookingConfirmedEvent(
            bookingId = bookingId,
            scheduleId = booking.schedule.id,
            seats = seatPositions,
            seatGrade = seatGrade
        ))

        log.info("예매 확정 - bookingId: {}, scheduleId: {}", bookingId, booking.schedule.id)

        return BookingResponse.from(booking)
    }

    @Transactional
    fun cancelBooking(bookingId: UUID, userId: Long, cancelReason: String? = null): BookingResponse {
        val booking = bookingRepository.findByIdOrNull(bookingId) ?: throw CustomException(ErrorCode.BOOKING_NOT_FOUND)
        bookingService.validateBookingOwner(booking, userId)

        return processCancellation(booking, userId, cancelReason)
    }

    @Transactional
    fun cancelBookingByAdmin(bookingId: UUID, cancelReason: String? = null): BookingResponse {
        val booking = bookingRepository.findByIdOrNull(bookingId) ?: throw CustomException(ErrorCode.BOOKING_NOT_FOUND)
        return processCancellation(booking, booking.siteUser.id, cancelReason ?: "관리자에 의한 강제 취소")
    }

    private fun processCancellation(booking: Booking, userId: Long, cancelReason: String?): BookingResponse {
        val bookingId = booking.id!!
        val scheduleId = booking.schedule.id
        val seatPositions = booking.bookingSeats.map { SeatPosition(it.row, it.col) }
        val currentStatus = booking.bookingStatus

        // 1. 결제 상태 확인 및 환불 (Facade 간의 조율)
        if (currentStatus == BookingStatus.CONFIRMED) {
            val payment = paymentRepository.findByBookingId(bookingId)
            if (payment != null && payment.paymentStatus == PaymentStatus.COMPLETED) {
                paymentFacade.cancelPayment(bookingId, cancelReason ?: "사용자 예매 취소 요청")
            }
        }

        // 2. 외부 이벤트 발행 (좌석 점유 해제)
        eventPublisher.publishEvent(BookingCancelledEvent(
            bookingId = bookingId,
            scheduleId = scheduleId,
            userId = userId,
            seats = seatPositions,
            previousStatus = currentStatus
        ))

        // 3. 예매 상태 변경 (BookingService 위임)
        if (currentStatus == BookingStatus.PENDING) {
            bookingService.releaseBookingInternal(booking)
        } else {
            bookingService.cancelBookingInternal(booking)
        }

        log.info("예매 취소 처리 완료 - bookingId: {}, 처리자ID: {}", bookingId, userId)

        return BookingResponse.from(booking)
    }
}
