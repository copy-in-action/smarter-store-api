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
    private val paymentRepository: PaymentRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 예매 데이터 생성 (Booking + BookingSeats)
     */
    @Transactional
    fun createAndSaveBooking(
        user: com.github.copyinaction.auth.domain.User,
        schedule: com.github.copyinaction.performance.domain.PerformanceSchedule,
        seats: List<SeatPositionRequest>
    ): Booking {
        val newBooking = Booking.create(user, schedule)
        val venue = schedule.performance.venue ?: throw CustomException(ErrorCode.VENUE_NOT_FOUND)
        val ticketOptions = ticketOptionRepository.findByPerformanceScheduleId(schedule.id)
        
        seats.forEach { seatRequest ->
            val grade = seatingChartParser.getSeatGrade(venue.seatingChart, seatRequest.row, seatRequest.col)
                ?: throw CustomException(
                    ErrorCode.INVALID_REQUEST,
                    "유효하지 않은 좌석입니다. (row: ${seatRequest.row}, col: ${seatRequest.col})"
                )

            val ticketOption = ticketOptions.find { it.seatGrade == grade }
                ?: throw CustomException(
                    ErrorCode.INVALID_REQUEST,
                    "해당 좌석 등급(${grade.name})에 대한 가격 정보가 없습니다."
                )

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

    /**
     * 예매 확정 처리 (도메인 상태 전환)
     */
    @Transactional
    fun confirmBookingInternal(booking: Booking) {
        booking.confirm()
        bookingRepository.save(booking)
    }

    /**
     * 예매 취소 처리 (도메인 상태 전환)
     */
    @Transactional
    fun cancelBookingInternal(booking: Booking) {
        booking.cancel()
        bookingRepository.save(booking)
    }

    /**
     * 예매 해제 처리 (PENDING 상태에서 취소될 때)
     */
    @Transactional
    fun releaseBookingInternal(booking: Booking) {
        booking.release()
        bookingRepository.save(booking)
    }

    /**
     * 예매 만료 처리
     */
    @Transactional
    fun expireBookingInternal(booking: Booking) {
        booking.expire()
        bookingRepository.save(booking)
    }

    @Transactional(readOnly = true)
    fun getRemainingTime(bookingId: UUID, userId: Long): BookingTimeResponse {
        val booking = bookingRepository.findByIdOrNull(bookingId) ?: throw CustomException(ErrorCode.BOOKING_NOT_FOUND)
        validateBookingOwner(booking, userId)

        return BookingTimeResponse.from(booking)
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

    fun validateBookingOwner(booking: Booking, userId: Long) {
        if (booking.siteUser.id != userId) {
            throw CustomException(ErrorCode.FORBIDDEN, "해당 예매에 대한 권한이 없습니다.")
        }
    }
}
