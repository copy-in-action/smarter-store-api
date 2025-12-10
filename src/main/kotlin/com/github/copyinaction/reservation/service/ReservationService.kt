package com.github.copyinaction.reservation.service

import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.reservation.domain.Reservation
import com.github.copyinaction.reservation.domain.ReservationStatus
import com.github.copyinaction.reservation.dto.CreateReservationRequest
import com.github.copyinaction.reservation.dto.ReservationResponse
import com.github.copyinaction.reservation.repository.ReservationRepository
import com.github.copyinaction.reservation.repository.ScheduleTicketStockRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class ReservationService(
    private val reservationRepository: ReservationRepository,
    private val scheduleTicketStockRepository: ScheduleTicketStockRepository
) {

    @Transactional
    fun createReservation(request: CreateReservationRequest, userId: Long? = null): ReservationResponse {
        // 비관적 락으로 재고 조회
        val stock = scheduleTicketStockRepository.findByIdWithLock(request.scheduleTicketStockId)
            ?: throw CustomException(ErrorCode.SCHEDULE_TICKET_STOCK_NOT_FOUND)

        // 잔여석 확인
        if (stock.remainingQuantity < request.quantity) {
            throw CustomException(ErrorCode.NOT_ENOUGH_SEATS)
        }

        // 재고 차감
        stock.decreaseStock(request.quantity)

        // 총 가격 계산
        val totalPrice = stock.ticketOption.price.multiply(request.quantity.toBigDecimal())

        // 예매 번호 생성
        val reservationNumber = generateReservationNumber()

        // 예매 생성
        val reservation = Reservation(
            reservationNumber = reservationNumber,
            scheduleTicketStock = stock,
            userId = userId,
            userName = request.userName,
            userPhone = request.userPhone,
            userEmail = request.userEmail,
            quantity = request.quantity,
            totalPrice = totalPrice,
            status = ReservationStatus.PENDING
        )

        val savedReservation = reservationRepository.save(reservation)
        return ReservationResponse.from(savedReservation)
    }

    @Transactional
    fun confirmReservation(reservationId: Long): ReservationResponse {
        val reservation = findReservationById(reservationId)
        reservation.confirm()
        return ReservationResponse.from(reservation)
    }

    @Transactional
    fun cancelReservation(reservationId: Long): ReservationResponse {
        val reservation = findReservationById(reservationId)

        // 이미 취소된 예매는 재고 복구하지 않음
        if (reservation.status == ReservationStatus.CANCELLED) {
            throw CustomException(ErrorCode.RESERVATION_ALREADY_CANCELLED)
        }

        // 재고 복구 (비관적 락)
        val stock = scheduleTicketStockRepository.findByIdWithLock(reservation.scheduleTicketStock!!.id)
            ?: throw CustomException(ErrorCode.SCHEDULE_TICKET_STOCK_NOT_FOUND)

        stock.increaseStock(reservation.quantity)
        reservation.cancel()

        return ReservationResponse.from(reservation)
    }

    fun getReservation(reservationId: Long): ReservationResponse {
        val reservation = findReservationById(reservationId)
        return ReservationResponse.from(reservation)
    }

    fun getReservationByNumber(reservationNumber: String): ReservationResponse {
        val reservation = reservationRepository.findByReservationNumberWithDetails(reservationNumber)
            ?: throw CustomException(ErrorCode.RESERVATION_NOT_FOUND)
        return ReservationResponse.from(reservation)
    }

    fun getReservationByNumberAndPhone(reservationNumber: String, userPhone: String): ReservationResponse {
        val reservation = reservationRepository.findByReservationNumberWithDetails(reservationNumber)
            ?: throw CustomException(ErrorCode.RESERVATION_NOT_FOUND)

        // 연락처 검증 (하이픈 제거 후 비교)
        val normalizedPhone = userPhone.replace("-", "")
        val reservationPhone = reservation.userPhone.replace("-", "")

        if (normalizedPhone != reservationPhone) {
            throw CustomException(ErrorCode.RESERVATION_NOT_FOUND)
        }

        return ReservationResponse.from(reservation)
    }

    fun getReservationsByUserId(userId: Long): List<ReservationResponse> {
        return reservationRepository.findByUserIdWithDetails(userId)
            .map { ReservationResponse.from(it) }
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
