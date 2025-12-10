package com.github.copyinaction.reservation.service

import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.reservation.domain.ScheduleSeat
import com.github.copyinaction.reservation.domain.SeatStatus
import com.github.copyinaction.reservation.dto.*
import com.github.copyinaction.reservation.repository.ScheduleSeatRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class SeatHoldService(
    private val scheduleSeatRepository: ScheduleSeatRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun holdSeats(
        scheduleId: Long,
        request: HoldSeatsRequest,
        userId: Long?
    ): HoldSeatsResponse {
        val scheduleSeatIds = request.scheduleSeatIds
        val sessionId = request.sessionId

        // 최대 좌석 수 제한 체크
        if (scheduleSeatIds.size > ScheduleSeat.MAX_SEATS_PER_USER) {
            throw CustomException(ErrorCode.MAX_SEATS_EXCEEDED)
        }

        // ID 순으로 정렬하여 비관적 락 획득 (데드락 방지)
        val sortedIds = scheduleSeatIds.sorted()
        val seats = scheduleSeatRepository.findByIdsWithLock(sortedIds)

        // 요청한 좌석이 모두 존재하는지 확인
        if (seats.size != scheduleSeatIds.size) {
            throw CustomException(ErrorCode.SCHEDULE_SEAT_NOT_FOUND)
        }

        // 모든 좌석이 해당 회차의 좌석인지 확인
        if (seats.any { it.schedule.id != scheduleId }) {
            throw CustomException(ErrorCode.SCHEDULE_SEAT_NOT_FOUND)
        }

        // 모든 좌석이 AVAILABLE 상태인지 확인
        val unavailableSeats = seats.filter { it.status != SeatStatus.AVAILABLE }
        if (unavailableSeats.isNotEmpty()) {
            throw CustomException(ErrorCode.SEAT_NOT_AVAILABLE)
        }

        // 좌석 점유
        seats.forEach { seat ->
            seat.hold(userId, sessionId, ScheduleSeat.DEFAULT_HOLD_MINUTES)
        }

        // 총 가격 계산
        val totalPrice = seats.fold(BigDecimal.ZERO) { acc, seat ->
            acc.add(seat.ticketOption.price)
        }

        // 응답 생성
        val heldSeats = seats.map { seat ->
            HeldSeatInfo(
                scheduleSeatId = seat.id,
                displayName = seat.getDisplayName(),
                ticketOptionName = seat.ticketOption.name,
                price = seat.ticketOption.price,
                heldUntil = seat.heldUntil!!
            )
        }

        logger.info("Seats held: userId=$userId, sessionId=$sessionId, seatIds=$sortedIds")

        return HoldSeatsResponse(
            success = true,
            heldSeats = heldSeats,
            totalPrice = totalPrice,
            expiresAt = seats.first().heldUntil!!
        )
    }

    @Transactional
    fun releaseSeats(
        scheduleId: Long,
        request: ReleaseSeatsRequest,
        userId: Long?
    ): ReleaseSeatsResponse {
        val scheduleSeatIds = request.scheduleSeatIds
        val sessionId = request.sessionId

        val sortedIds = scheduleSeatIds.sorted()
        val seats = scheduleSeatRepository.findByIdsWithLock(sortedIds)

        // 해당 회차의 좌석인지 확인
        val targetSeats = seats.filter { it.schedule.id == scheduleId }

        var releasedCount = 0
        targetSeats.forEach { seat ->
            // 본인이 점유한 좌석만 해제
            if (seat.isHeldBy(userId, sessionId)) {
                seat.release()
                releasedCount++
            }
        }

        logger.info("Seats released: userId=$userId, sessionId=$sessionId, releasedCount=$releasedCount")

        return ReleaseSeatsResponse(
            success = true,
            releasedCount = releasedCount
        )
    }

    @Transactional
    fun releaseAllHeldSeats(userId: Long?, sessionId: String?, scheduleId: Long): Int {
        val heldSeats = scheduleSeatRepository.findHeldSeatsByUser(scheduleId, userId, sessionId)

        heldSeats.forEach { it.release() }

        logger.info("All held seats released: userId=$userId, sessionId=$sessionId, count=${heldSeats.size}")

        return heldSeats.size
    }

    fun getHeldSeats(scheduleId: Long, userId: Long?, sessionId: String?): List<ScheduleSeatResponse> {
        val heldSeats = scheduleSeatRepository.findHeldSeatsByUser(scheduleId, userId, sessionId)

        return heldSeats.map { ScheduleSeatResponse.from(it, userId, sessionId) }
    }
}
