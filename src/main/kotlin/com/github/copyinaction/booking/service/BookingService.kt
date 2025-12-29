package com.github.copyinaction.booking.service

import com.github.copyinaction.auth.repository.UserRepository
import com.github.copyinaction.booking.domain.Booking
import com.github.copyinaction.booking.domain.BookingSeat
import com.github.copyinaction.booking.domain.BookingStatus
import com.github.copyinaction.booking.dto.BookingResponse
import com.github.copyinaction.booking.dto.BookingTimeResponse
import com.github.copyinaction.booking.dto.SeatPositionRequest
import com.github.copyinaction.booking.repository.BookingRepository
import com.github.copyinaction.booking.repository.BookingSeatRepository
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.performance.repository.PerformanceScheduleRepository
import com.github.copyinaction.performance.repository.TicketOptionRepository
import com.github.copyinaction.seat.domain.ScheduleSeatStatus
import com.github.copyinaction.seat.domain.SeatStatus
import com.github.copyinaction.seat.dto.SeatPosition
import com.github.copyinaction.seat.repository.ScheduleSeatStatusRepository
import com.github.copyinaction.seat.service.SseService
import com.github.copyinaction.venue.domain.SeatGrade
import com.github.copyinaction.venue.util.SeatingChartParser
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
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
    private val bookingSeatRepository: BookingSeatRepository,
    private val userRepository: UserRepository,
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val ticketOptionRepository: TicketOptionRepository,
    private val scheduleSeatStatusRepository: ScheduleSeatStatusRepository,
    private val sseService: SseService,
    private val seatingChartParser: SeatingChartParser
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 예매 시작 - 좌석 일괄 점유
     * 결제 진입 시점에 선택한 좌석들을 한꺼번에 점유합니다.
     */
    @Transactional
    fun startBooking(scheduleId: Long, seats: List<SeatPositionRequest>, userId: Long): BookingResponse {
        val user = userRepository.findByIdOrNull(userId) ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        val schedule = performanceScheduleRepository.findByIdOrNull(scheduleId)
            ?: throw CustomException(ErrorCode.PERFORMANCE_SCHEDULE_NOT_FOUND)

        val newSeatSet = seats.map { SeatPosition(it.row, it.col) }.toSet()

        // 1. 기존 PENDING 상태의 예매 확인 및 취소
        val existingBooking = bookingRepository.findByUser_IdAndSchedule_IdAndStatus(userId, scheduleId, BookingStatus.PENDING)
        val oldSeatSet = getOldSeatSet(existingBooking)

        if (existingBooking != null) {
            existingBooking.cancel()
            bookingRepository.save(existingBooking)
        }

        // 2. 좌석 변경 사항 계산
        val (keptSeats, releasedSeats, addedSeats) = calculateSeatChanges(oldSeatSet, newSeatSet)

        // 3. 좌석 상태 DB 반영
        processSeatChanges(schedule, userId, keptSeats, releasedSeats, addedSeats)

        // 4. Booking 및 BookingSeat 생성
        val savedBooking = createAndSaveBooking(user, schedule, seats)

        // 5. SSE 이벤트 발행
        publishSseEvents(scheduleId, releasedSeats, addedSeats)

        log.info("예매 시작 - bookingId: {}, scheduleId: {}, 유지: {}, 추가: {}, 해제: {}",
            savedBooking.id, scheduleId, keptSeats.size, addedSeats.size, releasedSeats.size)

        return BookingResponse.from(savedBooking)
    }

    private fun getOldSeatSet(booking: Booking?): Set<SeatPosition> {
        return booking?.bookingSeats?.map {
            SeatPosition(it.rowName.toIntOrNull() ?: 0, it.seatNumber)
        }?.toSet() ?: emptySet()
    }

    private fun calculateSeatChanges(oldSet: Set<SeatPosition>, newSet: Set<SeatPosition>): Triple<Set<SeatPosition>, Set<SeatPosition>, Set<SeatPosition>> {
        val kept = oldSet.intersect(newSet)
        val released = oldSet - newSet
        val added = newSet - oldSet
        return Triple(kept, released, added)
    }

    private fun processSeatChanges(
        schedule: com.github.copyinaction.performance.domain.PerformanceSchedule,
        userId: Long,
        keptSeats: Set<SeatPosition>,
        releasedSeats: Set<SeatPosition>,
        addedSeats: Set<SeatPosition>
    ) {
        val scheduleId = schedule.id
        try {
            // 해제될 좌석 삭제
            for (seat in releasedSeats) {
                val existingSeat = scheduleSeatStatusRepository.findByScheduleIdAndRowNumAndColNum(
                    scheduleId, seat.row, seat.col
                )
                if (existingSeat != null && existingSeat.heldBy == userId) {
                    scheduleSeatStatusRepository.delete(existingSeat)
                }
            }

            // 유지될 좌석 만료시간 연장
            for (seat in keptSeats) {
                scheduleSeatStatusRepository.findByScheduleIdAndRowNumAndColNum(
                    scheduleId, seat.row, seat.col
                )?.extendHold()
            }

            // 새로 추가될 좌석 점유
            val seatingChartJson = schedule.performance.venue?.seatingChart
            for (seat in addedSeats) {
                val existingSeat = scheduleSeatStatusRepository.findByScheduleIdAndRowNumAndColNum(
                    scheduleId, seat.row, seat.col
                )

                if (existingSeat != null) {
                    if (existingSeat.isExpired()) {
                        scheduleSeatStatusRepository.delete(existingSeat)
                        scheduleSeatStatusRepository.flush()
                    } else {
                        throw CustomException(ErrorCode.SEAT_ALREADY_OCCUPIED, "좌석(${seat.row}, ${seat.col})이 이미 점유되어 있습니다.")
                    }
                }

                val seatGrade = seatingChartParser.getSeatGrade(seatingChartJson, seat.row, seat.col)
                    ?: SeatGrade.R

                val seatStatus = ScheduleSeatStatus.hold(
                    schedule = schedule,
                    rowNum = seat.row,
                    colNum = seat.col,
                    seatGrade = seatGrade,
                    userId = userId
                )
                scheduleSeatStatusRepository.save(seatStatus)
            }
            scheduleSeatStatusRepository.flush()
        } catch (e: DataIntegrityViolationException) {
            log.warn("좌석 점유 충돌 - scheduleId: {}, userId: {}", scheduleId, userId)
            throw CustomException(ErrorCode.SEAT_ALREADY_OCCUPIED, "선택한 좌석 중 일부가 다른 사용자에게 점유되었습니다.")
        }
    }

    private fun createAndSaveBooking(
        user: com.github.copyinaction.auth.domain.User,
        schedule: com.github.copyinaction.performance.domain.PerformanceSchedule,
        seats: List<SeatPositionRequest>
    ): Booking {
        val bookingNumber = generateBookingNumber()
        val newBooking = Booking.create(user, schedule, bookingNumber)

        val defaultTicketOption = ticketOptionRepository.findByPerformanceScheduleId(schedule.id).firstOrNull()
        val seatGrade = defaultTicketOption?.seatGrade ?: com.github.copyinaction.venue.domain.SeatGrade.R
        val seatPrice = defaultTicketOption?.price ?: 0

        val seatDetails = seats.map { it.row to it.col }
        newBooking.addSeats(seatDetails, seatGrade, seatPrice)

        return bookingRepository.saveAndFlush(newBooking)
    }

    private fun publishSseEvents(
        scheduleId: Long,
        releasedSeats: Set<SeatPosition>,
        addedSeats: Set<SeatPosition>
    ) {
        if (releasedSeats.isNotEmpty()) {
            sseService.sendReleased(scheduleId, releasedSeats.toList())
        }
        if (addedSeats.isNotEmpty()) {
            sseService.sendOccupied(scheduleId, addedSeats.toList())
        }
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
        
        if (booking.isExpired()) { // 엔티티의 isExpired() 사용
            booking.expire() // 상태 변경
            throw CustomException(ErrorCode.BOOKING_EXPIRED)
        }

        // 예매 확정 시 영구 점유 처리
        booking.bookingSeats.forEach { bookingSeat ->
            val rowNum = bookingSeat.rowName.toIntOrNull()
                ?: throw CustomException(ErrorCode.INVALID_REQUEST, "유효하지 않은 좌석 열입니다.")

            val existingSeat = scheduleSeatStatusRepository.findByScheduleIdAndRowNumAndColNum(
                booking.schedule.id,
                rowNum,
                bookingSeat.seatNumber
            )

            if (existingSeat != null) {
                // 기존 좌석이 이미 RESERVED면 에러
                if (existingSeat.seatStatus == SeatStatus.RESERVED) {
                    throw CustomException(ErrorCode.SEAT_ALREADY_OCCUPIED, "결제 도중 이미 판매 완료된 좌석이 발생했습니다.")
                }
                existingSeat.seatStatus = SeatStatus.RESERVED
                scheduleSeatStatusRepository.save(existingSeat)
            } else {
                // 새로 생성 (seatGrade는 BookingSeat에서 조회)
                val newSeat = ScheduleSeatStatus(
                    schedule = booking.schedule,
                    rowNum = rowNum,
                    colNum = bookingSeat.seatNumber,
                    seatGrade = bookingSeat.grade,
                    seatStatus = SeatStatus.RESERVED
                )
                scheduleSeatStatusRepository.save(newSeat)
            }
        }

        booking.confirm() // Booking 엔티티의 confirm() 호출
        bookingRepository.save(booking)

        // SSE CONFIRMED 이벤트 발행
        val seatPositions = booking.bookingSeats.map {
            SeatPosition(it.rowName.toIntOrNull() ?: 1, it.seatNumber)
        }
        sseService.sendConfirmed(booking.schedule.id, seatPositions)

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

        // PENDING 상태인 경우 좌석 점유 해제
        if (booking.status == BookingStatus.PENDING) {
            scheduleSeatStatusRepository.deleteByScheduleIdAndHeldByAndSeatStatus(
                scheduleId, booking.user.id, SeatStatus.PENDING
            )
        }

        booking.cancel() // Booking 엔티티의 cancel() 호출
        bookingRepository.save(booking)

        // SSE RELEASED 이벤트 발행
        if (seatPositions.isNotEmpty()) {
            sseService.sendReleased(scheduleId, seatPositions)
        }

        log.info("예매 취소 - bookingId: {}, scheduleId: {}", bookingId, scheduleId)

        return BookingResponse.from(booking)
    }

    private fun generateBookingNumber(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val datePart = LocalDateTime.now().format(formatter)
        val randomPart = Random.nextInt(100_000_000, 999_999_999).toString() // 8자리 숫자
        return "$datePart-$randomPart"
    }

    private fun validateBookingOwner(booking: Booking, userId: Long) {
        if (booking.user.id != userId) {
            throw CustomException(ErrorCode.FORBIDDEN, "해당 예매에 대한 권한이 없습니다.")
        }
    }
}
