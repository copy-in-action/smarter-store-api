package com.github.copyinaction.seat.service

import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.performance.repository.PerformanceScheduleRepository
import com.github.copyinaction.seat.domain.ScheduleSeatStatus
import com.github.copyinaction.seat.domain.SeatStatus
import com.github.copyinaction.seat.dto.ScheduleSeatStatusResponse
import com.github.copyinaction.seat.dto.SeatHoldResponse
import com.github.copyinaction.seat.dto.SeatPositionRequest
import com.github.copyinaction.seat.dto.SeatStatusResponse
import com.github.copyinaction.seat.repository.ScheduleSeatStatusRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class SeatService(
    private val seatStatusRepository: ScheduleSeatStatusRepository,
    private val scheduleRepository: PerformanceScheduleRepository
) {

    companion object {
        private const val MAX_SEATS_PER_USER = 4
    }

    /**
     * 회차별 좌석 상태 조회
     * PENDING, RESERVED 상태인 좌석만 반환
     */
    fun getSeatStatus(scheduleId: Long): ScheduleSeatStatusResponse {
        // 회차 존재 확인
        if (!scheduleRepository.existsById(scheduleId)) {
            throw CustomException(ErrorCode.SCHEDULE_NOT_FOUND)
        }

        val seatStatuses = seatStatusRepository.findByScheduleId(scheduleId)

        return ScheduleSeatStatusResponse(
            scheduleId = scheduleId,
            seats = seatStatuses.map { SeatStatusResponse.from(it) }
        )
    }

    /**
     * 좌석 점유 (10분)
     */
    @Transactional
    fun holdSeats(scheduleId: Long, userId: Long, seats: List<SeatPositionRequest>): SeatHoldResponse {
        // 최대 좌석 수 검증
        if (seats.size > MAX_SEATS_PER_USER) {
            throw CustomException(ErrorCode.SEAT_LIMIT_EXCEEDED)
        }

        if (seats.isEmpty()) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE)
        }

        // 회차 조회
        val schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow { CustomException(ErrorCode.SCHEDULE_NOT_FOUND) }

        // 기존 점유 좌석 삭제 (같은 유저가 다시 점유 시도하는 경우)
        seatStatusRepository.deleteByScheduleIdAndHeldByAndSeatStatus(scheduleId, userId, SeatStatus.PENDING)

        // 좌석별 점유 가능 여부 확인 및 점유
        val heldSeats = mutableListOf<ScheduleSeatStatus>()

        for (seat in seats) {
            // 이미 점유/예약된 좌석인지 확인
            val existingSeat = seatStatusRepository.findByScheduleIdAndRowNumAndColNum(
                scheduleId, seat.row, seat.col
            )

            if (existingSeat != null) {
                // 만료된 점유인 경우 삭제
                if (existingSeat.isExpired()) {
                    seatStatusRepository.delete(existingSeat)
                } else {
                    throw CustomException(ErrorCode.SEAT_ALREADY_OCCUPIED)
                }
            }

            // 좌석 점유
            val seatStatus = ScheduleSeatStatus.hold(
                schedule = schedule,
                rowNum = seat.row,
                colNum = seat.col,
                userId = userId
            )
            heldSeats.add(seatStatusRepository.save(seatStatus))
        }

        val expiresAt = heldSeats.firstOrNull()?.heldUntil
            ?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) ?: ""

        return SeatHoldResponse(
            scheduleId = scheduleId,
            heldSeats = heldSeats.map { SeatStatusResponse.from(it) },
            expiresAt = expiresAt
        )
    }

    /**
     * 좌석 점유 해제
     */
    @Transactional
    fun releaseSeats(scheduleId: Long, userId: Long) {
        // 회차 존재 확인
        if (!scheduleRepository.existsById(scheduleId)) {
            throw CustomException(ErrorCode.SCHEDULE_NOT_FOUND)
        }

        seatStatusRepository.deleteByScheduleIdAndHeldByAndSeatStatus(scheduleId, userId, SeatStatus.PENDING)
    }

    /**
     * 좌석 예약 확정 (결제 완료)
     */
    @Transactional
    fun reserveSeats(scheduleId: Long, userId: Long): List<SeatStatusResponse> {
        // 회차 존재 확인
        if (!scheduleRepository.existsById(scheduleId)) {
            throw CustomException(ErrorCode.SCHEDULE_NOT_FOUND)
        }

        // 유저가 점유 중인 좌석 조회
        val pendingSeats = seatStatusRepository.findByScheduleIdAndHeldByAndSeatStatus(
            scheduleId, userId, SeatStatus.PENDING
        )

        if (pendingSeats.isEmpty()) {
            throw CustomException(ErrorCode.NO_SEATS_TO_RESERVE)
        }

        // 만료된 좌석 확인
        val expiredSeats = pendingSeats.filter { it.isExpired() }
        if (expiredSeats.isNotEmpty()) {
            // 만료된 좌석 삭제
            seatStatusRepository.deleteAll(expiredSeats)
            throw CustomException(ErrorCode.SEAT_HOLD_EXPIRED)
        }

        // 예약 확정
        pendingSeats.forEach { it.reserve() }

        return pendingSeats.map { SeatStatusResponse.from(it) }
    }

    /**
     * 만료된 점유 좌석 정리 (스케줄러에서 호출)
     */
    @Transactional
    fun cleanupExpiredHolds(): Int {
        return seatStatusRepository.deleteExpiredHolds(java.time.LocalDateTime.now())
    }
}
