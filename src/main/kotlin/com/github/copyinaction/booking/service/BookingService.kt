package com.github.copyinaction.booking.service

import com.github.copyinaction.auth.repository.UserRepository
import com.github.copyinaction.booking.domain.Booking
import com.github.copyinaction.booking.domain.BookingCancelledEvent
import com.github.copyinaction.booking.domain.BookingConfirmedEvent
import com.github.copyinaction.booking.domain.BookingStartedEvent
import com.github.copyinaction.booking.domain.BookingStatus
import com.github.copyinaction.booking.dto.AdminBookingResponse
import com.github.copyinaction.booking.dto.BookingDetailResponse
import com.github.copyinaction.booking.dto.BookingHistoryResponse
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
import com.github.copyinaction.booking.domain.BookingSeat
import com.github.copyinaction.payment.domain.PaymentStatus
import com.github.copyinaction.payment.domain.Payment
import com.github.copyinaction.payment.repository.PaymentRepository
import com.github.copyinaction.payment.service.PaymentService
import com.github.copyinaction.venue.util.SeatingChartParser
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
    private val ticketOptionRepository: TicketOptionRepository,
    private val seatingChartParser: SeatingChartParser,
    private val paymentRepository: PaymentRepository,
    private val paymentService: PaymentService
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
            SeatPosition(it.row, it.col)
        }?.toSet() ?: emptySet()
    }

    private fun createAndSaveBooking(
        user: com.github.copyinaction.auth.domain.User,
        schedule: com.github.copyinaction.performance.domain.PerformanceSchedule,
        seats: List<SeatPositionRequest>
    ): Booking {
        val newBooking = Booking.create(user, schedule)
        val venue = schedule.performance.venue ?: throw CustomException(ErrorCode.VENUE_NOT_FOUND)
        val ticketOptions = ticketOptionRepository.findByPerformanceScheduleId(schedule.id)
        
        seats.forEach { seatRequest ->
            // 좌석 등급 파싱
            val grade = seatingChartParser.getSeatGrade(venue.seatingChart, seatRequest.row, seatRequest.col)
                ?: throw CustomException(
                    ErrorCode.INVALID_REQUEST,
                    "유효하지 않은 좌석입니다. (row: ${seatRequest.row}, col: ${seatRequest.col})"
                )

            // 해당 등급의 티켓 옵션(가격) 찾기
            val ticketOption = ticketOptions.find { it.seatGrade == grade }
                ?: throw CustomException(
                    ErrorCode.INVALID_REQUEST,
                    "해당 좌석 등급(${grade.name})에 대한 가격 정보가 없습니다."
                )

            // BookingSeat 생성 (create 내부에서 booking.addSeat 호출)
            BookingSeat.create(
                booking = newBooking,
                section = BookingSeat.DEFAULT_SECTION,
                row = seatRequest.row,
                col = seatRequest.col,
                grade = grade,
                price = ticketOption.price
            )
        }

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
            SeatPosition(it.row, it.col)
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
    fun cancelBookingByAdmin(bookingId: UUID, cancelReason: String? = null): BookingResponse {
        val booking = bookingRepository.findByIdOrNull(bookingId) ?: throw CustomException(ErrorCode.BOOKING_NOT_FOUND)
        
        // 관리자 취소는 소유권 검증(validateBookingOwner)을 건너뜁니다.
        return processCancellation(booking, booking.siteUser.id, cancelReason ?: "관리자에 의한 강제 취소")
    }

    @Transactional
    fun cancelBooking(bookingId: UUID, userId: Long, cancelReason: String? = null): BookingResponse {
        val booking = bookingRepository.findByIdOrNull(bookingId) ?: throw CustomException(ErrorCode.BOOKING_NOT_FOUND)
        validateBookingOwner(booking, userId)

        return processCancellation(booking, userId, cancelReason)
    }

    private fun processCancellation(booking: Booking, userId: Long, cancelReason: String?): BookingResponse {
        val bookingId = booking.id!!
        val scheduleId = booking.schedule.id
        val seatPositions = booking.bookingSeats.map {
            SeatPosition(it.row, it.col)
        }
        val currentStatus = booking.bookingStatus

        // 결제 상태 확인 및 환불 (CONFIRMED 상태일 때)
        if (currentStatus == BookingStatus.CONFIRMED) {
            val payment = paymentRepository.findByBookingId(bookingId)
            if (payment != null && payment.paymentStatus == PaymentStatus.COMPLETED) {
                paymentService.cancelPaymentInternal(bookingId, cancelReason ?: "사용자 예매 취소 요청")
            }
        }

        // 좌석 점유 해제를 위한 이벤트 등록 (PENDING, CONFIRMED 모두)
        booking.registerEvent(BookingCancelledEvent(
            bookingId = bookingId,
            scheduleId = scheduleId,
            userId = userId,
            seats = seatPositions,
            previousStatus = currentStatus
        ))

        booking.cancel()
        bookingRepository.save(booking)

        log.info("예매 취소 처리 완료 - bookingId: {}, 처리자ID: {}", bookingId, userId)

        return BookingResponse.from(booking)
    }

    @Transactional(readOnly = true)
    fun getMyBookings(userId: Long): List<BookingHistoryResponse> {
        return bookingRepository.findAllMyBookings(userId)
            .map { BookingHistoryResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getScheduleBookingsForAdmin(scheduleId: Long): List<AdminBookingResponse> {
        val bookings = bookingRepository.findAllByScheduleIdForAdmin(scheduleId)
        val bookingIds = bookings.mapNotNull { it.id }
        
        // 결제 정보 한꺼번에 조회 (N+1 방지)
        val payments = if (bookingIds.isNotEmpty()) {
            paymentRepository.findAllByBookingIdIn(bookingIds).associateBy { it.booking.id }
        } else {
            emptyMap()
        }

        return bookings.map { booking ->
            AdminBookingResponse.from(booking, payments[booking.id])
        }
    }

    @Transactional(readOnly = true)
    fun getBookingDetail(bookingId: UUID, userId: Long): BookingDetailResponse {
        val booking = bookingRepository.findByIdWithDetails(bookingId) ?: throw CustomException(ErrorCode.BOOKING_NOT_FOUND)
        validateBookingOwner(booking, userId)

        val payment = paymentRepository.findByBookingId(bookingId)
        return BookingDetailResponse.from(booking, payment, isAdmin = false)
    }

    @Transactional(readOnly = true)
    fun getBookingDetailForAdmin(bookingId: UUID): BookingDetailResponse {
        val booking = bookingRepository.findByIdWithDetails(bookingId) ?: throw CustomException(ErrorCode.BOOKING_NOT_FOUND)
        val payment = paymentRepository.findByBookingId(bookingId)
        
        return BookingDetailResponse.from(booking, payment, isAdmin = true)
    }

    private fun validateBookingOwner(booking: Booking, userId: Long) {
        if (booking.siteUser.id != userId) {
            throw CustomException(ErrorCode.FORBIDDEN, "해당 예매에 대한 권한이 없습니다.")
        }
    }
}
