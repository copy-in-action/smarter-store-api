package com.github.copyinaction.reservation.service

import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.reservation.domain.Reservation
import com.github.copyinaction.reservation.domain.ReservationSeat
import com.github.copyinaction.reservation.domain.ReservationStatus
import com.github.copyinaction.reservation.domain.SeatStatus
import com.github.copyinaction.reservation.dto.CreateSeatReservationRequest
import com.github.copyinaction.reservation.dto.SeatReservationResponse
import com.github.copyinaction.reservation.repository.ReservationRepository
import com.github.copyinaction.reservation.repository.ReservationSeatRepository
import com.github.copyinaction.reservation.repository.ScheduleSeatRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class SeatReservationService(
    private val reservationRepository: ReservationRepository,
    private val reservationSeatRepository: ReservationSeatRepository,
    private val scheduleSeatRepository: ScheduleSeatRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createSeatReservation(
        request: CreateSeatReservationRequest,
        userId: Long?
    ): SeatReservationResponse {
        val scheduleSeatIds = request.scheduleSeatIds
        val sessionId = request.sessionId

        // ID 순으로 정렬하여 비관적 락 획득 (데드락 방지)
        val sortedIds = scheduleSeatIds.sorted()
        val seats = scheduleSeatRepository.findByIdsWithLock(sortedIds)

        // 요청한 좌석이 모두 존재하는지 확인
        if (seats.size != scheduleSeatIds.size) {
            throw CustomException(ErrorCode.SCHEDULE_SEAT_NOT_FOUND)
        }

        // 모든 좌석이 같은 회차인지 확인
        val scheduleIds = seats.map { it.schedule.id }.distinct()
        if (scheduleIds.size != 1) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE)
        }

        // 상태 확인 (HELD 상태이고 본인이 점유 중이거나, AVAILABLE 상태)
        seats.forEach { seat ->
            val isValid = when (seat.status) {
                SeatStatus.AVAILABLE -> true
                SeatStatus.HELD -> seat.isHeldBy(userId, sessionId)
                SeatStatus.RESERVED -> false
                SeatStatus.UNAVAILABLE -> false
                else -> false
            }
            if (!isValid) {
                throw CustomException(ErrorCode.SEAT_NOT_AVAILABLE)
            }
        }

        // 총 가격 계산
        val totalPrice = seats.fold(BigDecimal.ZERO) { acc, seat ->
            acc.add(seat.ticketOption.price)
        }

        // 좌석 상태 변경
        seats.forEach { it.reserve() }

        // 예매 번호 생성
        val reservationNumber = generateReservationNumber()

        // 예매 생성
        val schedule = seats.first().schedule
        val reservation = Reservation(
            reservationNumber = reservationNumber,
            schedule = schedule,
            userId = userId,
            userName = request.userName,
            userPhone = request.userPhone,
            userEmail = request.userEmail,
            quantity = seats.size,
            totalPrice = totalPrice,
            status = ReservationStatus.PENDING
        )

        val savedReservation = reservationRepository.save(reservation)

        // 예매-좌석 연결 생성
        val reservationSeats = seats.map { seat ->
            ReservationSeat(
                reservation = savedReservation,
                scheduleSeat = seat
            )
        }
        reservationSeatRepository.saveAll(reservationSeats)

        logger.info("Seat reservation created: reservationNumber=$reservationNumber, userId=$userId, seatIds=$sortedIds")

        return SeatReservationResponse.from(savedReservation, reservationSeats)
    }

    @Transactional
    fun confirmSeatReservation(reservationId: Long): SeatReservationResponse {
        val reservation = findReservationById(reservationId)

        if (!reservation.isSeatBasedReservation()) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE)
        }

        reservation.confirm()

        val reservationSeats = reservationSeatRepository.findByReservationIdWithDetails(reservationId)

        logger.info("Seat reservation confirmed: reservationId=$reservationId")

        return SeatReservationResponse.from(reservation, reservationSeats)
    }

    @Transactional
    fun cancelSeatReservation(reservationId: Long): SeatReservationResponse {
        val reservation = findReservationById(reservationId)

        if (!reservation.isSeatBasedReservation()) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE)
        }

        if (reservation.status == ReservationStatus.CANCELLED) {
            throw CustomException(ErrorCode.RESERVATION_ALREADY_CANCELLED)
        }

        // 좌석 상태 복구
        val reservationSeats = reservationSeatRepository.findByReservationIdWithDetails(reservationId)
        reservationSeats.forEach { rs ->
            val scheduleSeat = scheduleSeatRepository.findByIdWithLock(rs.scheduleSeat.id)
            scheduleSeat?.cancelReservation()
        }

        reservation.cancel()

        logger.info("Seat reservation cancelled: reservationId=$reservationId")

        return SeatReservationResponse.from(reservation, reservationSeats)
    }

    fun getSeatReservation(reservationId: Long): SeatReservationResponse {
        val reservation = findReservationById(reservationId)

        if (!reservation.isSeatBasedReservation()) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE)
        }

        val reservationSeats = reservationSeatRepository.findByReservationIdWithDetails(reservationId)

        return SeatReservationResponse.from(reservation, reservationSeats)
    }

    fun getSeatReservationByNumberAndPhone(
        reservationNumber: String,
        userPhone: String
    ): SeatReservationResponse {
        val reservation = reservationRepository.findByReservationNumberWithDetails(reservationNumber)
            ?: throw CustomException(ErrorCode.RESERVATION_NOT_FOUND)

        if (!reservation.isSeatBasedReservation()) {
            throw CustomException(ErrorCode.RESERVATION_NOT_FOUND)
        }

        // 연락처 검증 (하이픈 제거 후 비교)
        val normalizedPhone = userPhone.replace("-", "")
        val reservationPhone = reservation.userPhone.replace("-", "")

        if (normalizedPhone != reservationPhone) {
            throw CustomException(ErrorCode.RESERVATION_NOT_FOUND)
        }

        val reservationSeats = reservationSeatRepository.findByReservationIdWithDetails(reservation.id)

        return SeatReservationResponse.from(reservation, reservationSeats)
    }

    fun getSeatReservationsByUserId(userId: Long): List<SeatReservationResponse> {
        val reservations = reservationRepository.findByUserIdWithDetails(userId)
            .filter { it.isSeatBasedReservation() }

        return reservations.map { reservation ->
            val reservationSeats = reservationSeatRepository.findByReservationIdWithDetails(reservation.id)
            SeatReservationResponse.from(reservation, reservationSeats)
        }
    }

    private fun findReservationById(id: Long): Reservation {
        return reservationRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.RESERVATION_NOT_FOUND) }
    }

    private fun generateReservationNumber(): String {
        val now = LocalDateTime.now()
        val dateTime = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val random = (1000..9999).random()
        return "R$dateTime$random"
    }
}
