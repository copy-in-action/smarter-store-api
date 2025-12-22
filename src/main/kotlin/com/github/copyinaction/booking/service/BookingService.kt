package com.github.copyinaction.booking.service

import com.github.copyinaction.auth.domain.User
import com.github.copyinaction.auth.repository.UserRepository
import com.github.copyinaction.booking.domain.Booking
import com.github.copyinaction.booking.domain.BookingSeat
import com.github.copyinaction.booking.domain.BookingStatus
import com.github.copyinaction.booking.domain.SeatLock
import com.github.copyinaction.booking.dto.BookingResponse
import com.github.copyinaction.booking.dto.BookingTimeResponse
import com.github.copyinaction.booking.dto.SeatRequest
import com.github.copyinaction.booking.repository.BookingRepository
import com.github.copyinaction.booking.repository.BookingSeatRepository
import com.github.copyinaction.booking.repository.SeatLockRepository
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.performance.domain.PerformanceSchedule
import com.github.copyinaction.performance.repository.PerformanceScheduleRepository
import com.github.copyinaction.performance.repository.TicketOptionRepository
import com.github.copyinaction.seat.domain.ScheduleSeatStatus
import com.github.copyinaction.seat.domain.SeatStatus
import com.github.copyinaction.seat.repository.ScheduleSeatStatusRepository
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
    private val seatLockRepository: SeatLockRepository,
    private val userRepository: UserRepository,
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val ticketOptionRepository: TicketOptionRepository,
    private val scheduleSeatStatusRepository: ScheduleSeatStatusRepository
) {

    @Transactional
    fun startBooking(scheduleId: Long, userId: Long): BookingResponse {
        val user = userRepository.findByIdOrNull(userId) ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        val schedule = performanceScheduleRepository.findByIdOrNull(scheduleId)
            ?: throw CustomException(ErrorCode.PERFORMANCE_SCHEDULE_NOT_FOUND)

        // 이미 PENDING 상태의 예매가 있는지 확인
        val existingBooking = bookingRepository.findByUser_IdAndSchedule_IdAndStatus(userId, scheduleId, BookingStatus.PENDING)
        if (existingBooking != null && !existingBooking.isExpired()) {
            return BookingResponse.from(existingBooking)
        }

        val bookingNumber = generateBookingNumber()
        val newBooking = Booking.create(user, schedule, bookingNumber)
        val savedBooking = bookingRepository.saveAndFlush(newBooking)

        return BookingResponse.from(savedBooking)
    }

    @Transactional
    fun selectSeat(bookingId: UUID, seatRequest: SeatRequest, userId: Long): BookingResponse {
        val booking = bookingRepository.findByIdOrNull(bookingId) ?: throw CustomException(ErrorCode.BOOKING_NOT_FOUND)
        validateBookingOwner(booking, userId)
        booking.validateBookingIsMutable() // 풍부한 도메인 모델의 유효성 검증

        // 1. 가격 검증 및 좌석 등급 정보 획득
        val ticketOption = ticketOptionRepository.findByPerformanceScheduleId(booking.schedule.id)
            .find { it.seatGrade.name == seatRequest.section } // TODO: section -> seatGrade 매핑 필요
            ?: throw CustomException(ErrorCode.INVALID_REQUEST, "유효하지 않은 좌석 구역입니다.")

        val seatPrice = ticketOption.price // 서버 기준 가격

        // 2. ScheduleSeatStatus에서 좌석이 판매 완료되었는지 확인
        // TODO: rowNum, colNum과 section, rowName, seatNumber 매핑 로직 필요
        val scheduleSeat = scheduleSeatStatusRepository.findByScheduleIdAndRowNumAndColNum(
            booking.schedule.id,
            seatRequest.rowName.toIntOrNull() ?: throw CustomException(ErrorCode.INVALID_REQUEST, "유효하지 않은 좌석 열입니다."),
            seatRequest.seatNumber
        )
        if (scheduleSeat != null && scheduleSeat.seatStatus == SeatStatus.RESERVED) {
            throw CustomException(ErrorCode.SEAT_ALREADY_OCCUPIED, "이미 판매된 좌석입니다.")
        }
        
        // 3. SeatLock 시도 (동시성 제어)
        // BookingSeat에 이미 이 좌석이 추가되어 있는지 확인 (중복 선택 방지)
        if (booking.bookingSeats.any { it.section == seatRequest.section && it.rowName == seatRequest.rowName && it.seatNumber == seatRequest.seatNumber }) {
            throw CustomException(ErrorCode.INVALID_REQUEST, "이미 선택된 좌석입니다.")
        }

        try {
            val seatLock = SeatLock.create(
                booking.schedule,
                booking,
                seatRequest.section,
                seatRequest.rowName,
                seatRequest.seatNumber
            )
            seatLockRepository.save(seatLock)
        } catch (e: Exception) {
            // Unique Constraint Violation 등의 예외 처리
            throw CustomException(ErrorCode.SEAT_ALREADY_OCCUPIED, "다른 사용자가 이미 선점한 좌석입니다.")
        }

        // 4. BookingSeat 추가 (Booking 엔티티 내부에서 4석 제한 검증 및 totalPrice 업데이트)
        val bookingSeat = BookingSeat(
            booking = booking,
            section = seatRequest.section,
            rowName = seatRequest.rowName,
            seatNumber = seatRequest.seatNumber,
            grade = ticketOption.seatGrade, // TODO: SeatGrade 매핑
            price = seatPrice
        )
        booking.addSeat(bookingSeat) // Booking 엔티티 내에서 addSeat 호출 (price, count 업데이트)
        bookingSeatRepository.save(bookingSeat) // cascade persist 되어도 명시적 save 권장

        bookingRepository.save(booking) // bookingSeats 변경 및 totalPrice 변경 사항 반영

        return BookingResponse.from(booking)
    }

    @Transactional
    fun deselectSeat(bookingId: UUID, seatRequest: SeatRequest, userId: Long): BookingResponse {
        val booking = bookingRepository.findByIdOrNull(bookingId) ?: throw CustomException(ErrorCode.BOOKING_NOT_FOUND)
        validateBookingOwner(booking, userId)
        booking.validateBookingIsMutable()

        // 1. BookingSeat 삭제
        val seatToRemove = booking.bookingSeats.find {
            it.section == seatRequest.section && it.rowName == seatRequest.rowName && it.seatNumber == seatRequest.seatNumber
        } ?: throw CustomException(ErrorCode.INVALID_REQUEST, "예매에 포함되지 않은 좌석입니다.")

        booking.removeSeat(seatToRemove)
        bookingSeatRepository.delete(seatToRemove)

        // 2. SeatLock 삭제
        seatLockRepository.deleteAllByBooking_Id(bookingId) // 해당 Booking이 보유한 모든 SeatLock을 삭제
        // TODO: 특정 좌석 하나만 삭제하는 로직으로 수정 필요
        // val seatLockToRemove = seatLockRepository.findByBookingIdAnd... (custom query 필요)
        // seatLockRepository.delete(seatLockToRemove)


        bookingRepository.save(booking) // totalPrice 변경 사항 반영

        return BookingResponse.from(booking)
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
                // 새로 생성
                val newSeat = ScheduleSeatStatus(
                    schedule = booking.schedule,
                    rowNum = rowNum,
                    colNum = bookingSeat.seatNumber,
                    seatStatus = SeatStatus.RESERVED
                )
                scheduleSeatStatusRepository.save(newSeat)
            }
        }

        booking.confirm() // Booking 엔티티의 confirm() 호출
        
        // 해당 Booking의 모든 SeatLock 삭제
        seatLockRepository.deleteAllByBooking_Id(bookingId)

        bookingRepository.save(booking)

        return BookingResponse.from(booking)
    }

    @Transactional
    fun cancelBooking(bookingId: UUID, userId: Long): BookingResponse {
        val booking = bookingRepository.findByIdOrNull(bookingId) ?: throw CustomException(ErrorCode.BOOKING_NOT_FOUND)
        validateBookingOwner(booking, userId)

        booking.cancel() // Booking 엔티티의 cancel() 호출

        // 해당 Booking의 모든 SeatLock 삭제
        seatLockRepository.deleteAllByBooking_Id(bookingId)

        // TODO: 만약 예약이 CONFIRMED 상태였다면, ScheduleSeatStatus에서 'SOLD' 상태를 다시 'AVAILABLE'로 되돌려야 함.
        // 현재는 PENDING 상태의 취소만 고려됨.
        
        bookingRepository.save(booking)

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
