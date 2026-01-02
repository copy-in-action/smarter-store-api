package com.github.copyinaction.booking.service

import com.github.copyinaction.auth.repository.UserRepository
import com.github.copyinaction.booking.domain.Booking
import com.github.copyinaction.booking.domain.BookingCancelledEvent
import com.github.copyinaction.booking.domain.BookingConfirmedEvent
import com.github.copyinaction.booking.domain.BookingStartedEvent
import com.github.copyinaction.booking.domain.BookingStatus
import com.github.copyinaction.booking.dto.BookingResponse
import com.github.copyinaction.booking.dto.BookingTimeResponse
import com.github.copyinaction.booking.dto.SeatPositionRequest
import com.github.copyinaction.booking.repository.BookingRepository
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.performance.repository.PerformanceScheduleRepository
import com.github.copyinaction.performance.repository.TicketOptionRepository
import com.github.copyinaction.seat.domain.SeatPosition
import com.github.copyinaction.seat.domain.SeatSelection
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.random.Random

@Service
class BookingService(
    private val bookingRepository: BookingRepository,
    private val userRepository: UserRepository,
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val ticketOptionRepository: TicketOptionRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 예매 시작 - 좌석 일괄 점유
     */
    @Transactional
    fun startBooking(scheduleId: Long, seats: List<SeatPositionRequest>, userId: Long): BookingResponse {
        val user = userRepository.findByIdOrNull(userId) ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        val schedule = performanceScheduleRepository.findByIdOrNull(scheduleId)
            ?: throw CustomException(ErrorCode.PERFORMANCE_SCHEDULE_NOT_FOUND)

        val newSeatSet = seats.map { SeatPosition(it.row, it.col) }.toSet()

        // 1. 기존 PENDING 상태의 예매 확인 및 취소
        val existingBooking = bookingRepository.findBySiteUser_IdAndSchedule_IdAndBookingStatus(userId, scheduleId, BookingStatus.PENDING)
        val oldSeatSet = getOldSeatSet(existingBooking)

        if (existingBooking != null) {
            existingBooking.cancel()
            bookingRepository.save(existingBooking)
        }

        // 2. 좌석 변경 사항 계산
        val oldSelection = SeatSelection(oldSeatSet)
        val newSelection = SeatSelection(newSeatSet)
        val seatChanges = oldSelection.calculateChanges(newSelection)

        // 3. Booking 및 BookingSeat 생성
        val savedBooking = createAndSaveBooking(user, schedule, seats)

        // 4. 좌석 점유를 위한 도메인 이벤트 등록
        savedBooking.registerEvent(BookingStartedEvent(
            bookingId = savedBooking.id!!,
            scheduleId = scheduleId,
            userId = userId,
            seatChanges = seatChanges
        ))
        
        // save를 명시적으로 호출하여 이벤트 발행 유도
        bookingRepository.save(savedBooking)

        log.info("예매 시작 - bookingId: {}, scheduleId: {}, 유지: {}, 추가: {}, 해제: {}",
            savedBooking.id, scheduleId, seatChanges.kept.size, seatChanges.added.size, seatChanges.released.size)

        return BookingResponse.from(savedBooking)
    }

    private fun getOldSeatSet(booking: Booking?): Set<SeatPosition> {
        return booking?.bookingSeats?.map {
            SeatPosition(it.rowName.toIntOrNull() ?: 0, it.seatNumber)
        }?.toSet() ?: emptySet()
    }

    private fun createAndSaveBooking(
        user: com.github.copyinaction.auth.domain.User,
        schedule: com.github.copyinaction.performance.domain.PerformanceSchedule,
        seats: List<SeatPositionRequest>
    ): Booking {
        val newBooking = Booking.create(user, schedule)

        val defaultTicketOption = ticketOptionRepository.findByPerformanceScheduleId(schedule.id).firstOrNull()
        val seatGrade = defaultTicketOption?.seatGrade ?: com.github.copyinaction.venue.domain.SeatGrade.R
        val seatPrice = defaultTicketOption?.price ?: 0

        val seatDetails = seats.map { it.row to it.col }
        newBooking.addSeats(seatDetails, seatGrade, seatPrice)

        return bookingRepository.saveAndFlush(newBooking)
    }

    @Transactional(readOnly = true)
    fun getRemainingTime(bookingId: UUID, userId: Long): BookingTimeResponse {
        val booking = bookingRepository.findByIdOrNull(bookingId) ?: throw CustomException(ErrorCode.BOOKING_NOT_FOUND)
        validateBookingOwner(booking, userId)

        return BookingTimeResponse.from(booking)
    }

    @Transactional
    fun confirmBooking(bookingId: UUID, userId: Long): BookingResponse {
        val booking = bookingRepository.findByIdOrNull(bookingId) ?: throw CustomException(ErrorCode.BOOKING_NOT_FOUND)
        validateBookingOwner(booking, userId)

        if (booking.isExpired()) {
            booking.expire()
            bookingRepository.save(booking) // 만료 상태 저장
            throw CustomException(ErrorCode.BOOKING_EXPIRED)
        }

        // 좌석 확정 처리 정보 추출
        val seatPositions = booking.bookingSeats.map {
            val rowNum = it.rowName.toIntOrNull()
                ?: throw CustomException(ErrorCode.INVALID_REQUEST, "유효하지 않은 좌석 열입니다.")
            SeatPosition(rowNum, it.seatNumber)
        }

        val seatGrade = booking.bookingSeats.firstOrNull()?.grade
            ?: com.github.copyinaction.venue.domain.SeatGrade.R

        // 1. 도메인 이벤트 등록
        booking.registerEvent(BookingConfirmedEvent(
            bookingId = bookingId,
            scheduleId = booking.schedule.id,
            seats = seatPositions,
            seatGrade = seatGrade
        ))

        // 2. 예매 상태 변경 (내부에서 검증 포함)
        booking.confirm()
        
        // 3. 저장 (이벤트 발행)
        bookingRepository.save(booking)

        log.info("예매 확정 - bookingId: {}, scheduleId: {}", bookingId, booking.schedule.id)

        return BookingResponse.from(booking)
    }

    @Transactional
    fun cancelBooking(bookingId: UUID, userId: Long): BookingResponse {
        val booking = bookingRepository.findByIdOrNull(bookingId) ?: throw CustomException(ErrorCode.BOOKING_NOT_FOUND)
        validateBookingOwner(booking, userId)

        val scheduleId = booking.schedule.id
        val seatPositions = booking.bookingSeats.map {
            SeatPosition(it.rowName.toIntOrNull() ?: 1, it.seatNumber)
        }

        // PENDING 상태인 경우 좌석 점유 해제를 위한 이벤트 등록
        if (booking.bookingStatus == BookingStatus.PENDING) {
            booking.registerEvent(BookingCancelledEvent(
                bookingId = bookingId,
                scheduleId = scheduleId,
                userId = userId,
                seats = seatPositions
            ))
        }

        booking.cancel()
        bookingRepository.save(booking)

        log.info("예매 취소 - bookingId: {}, scheduleId: {}", bookingId, scheduleId)

        return BookingResponse.from(booking)
    }

    private fun validateBookingOwner(booking: Booking, userId: Long) {
        if (booking.siteUser.id != userId) {
            throw CustomException(ErrorCode.FORBIDDEN, "해당 예매에 대한 권한이 없습니다.")
        }
    }
}
