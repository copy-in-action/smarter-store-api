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

@Service
@Transactional(readOnly = true)
class ReservationService(
    private val reservationRepository: ReservationRepository,
    private val scheduleTicketStockRepository: ScheduleTicketStockRepository
) {

    @Transactional
    fun createReservation(request: CreateReservationRequest, userId: Long? = null): ReservationResponse {
        val stock = scheduleTicketStockRepository.findByIdWithLock(request.scheduleTicketStockId)
            ?: throw CustomException(ErrorCode.SCHEDULE_TICKET_STOCK_NOT_FOUND)

        // 도메인에서 예매 생성 (재고 검증, 차감, 가격 계산 포함)
        val reservation = Reservation.createWithStock(
            stock = stock,
            userId = userId,
            userName = request.userName,
            userPhone = request.userPhone,
            userEmail = request.userEmail,
            quantity = request.quantity
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

        if (reservation.status == ReservationStatus.CANCELLED) {
            throw CustomException(ErrorCode.RESERVATION_ALREADY_CANCELLED)
        }

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

        // 도메인 메서드로 연락처 검증
        if (!reservation.matchesPhone(userPhone)) {
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
}
