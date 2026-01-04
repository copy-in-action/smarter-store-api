package com.github.copyinaction.seat.service

import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.performance.repository.PerformanceScheduleRepository
import com.github.copyinaction.seat.dto.ScheduleSeatStatusResponse
import com.github.copyinaction.seat.dto.SeatPositionResponse
import com.github.copyinaction.seat.dto.SeatStatusResponse
import com.github.copyinaction.seat.repository.ScheduleSeatStatusRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

import com.github.copyinaction.seat.domain.SeatStatus

@Service
@Transactional(readOnly = true)
class SeatService(
    private val seatStatusRepository: ScheduleSeatStatusRepository,
    private val scheduleRepository: PerformanceScheduleRepository
) {

    /**
     * 회차별 좌석 상태 조회
     * PENDING, RESERVED 상태인 좌석만 반환
     */
    fun getSeatStatus(scheduleId: Long): ScheduleSeatStatusResponse {
        if (!scheduleRepository.existsById(scheduleId)) {
            throw CustomException(ErrorCode.SCHEDULE_NOT_FOUND)
        }

        val seatStatuses = seatStatusRepository.findByScheduleId(scheduleId)
        val groupedSeats = seatStatuses.groupBy { it.seatStatus }

        return ScheduleSeatStatusResponse(
            scheduleId = scheduleId,
            pending = groupedSeats[SeatStatus.PENDING]?.map { SeatPositionResponse(it.rowNum, it.colNum) } ?: emptyList(),
            reserved = groupedSeats[SeatStatus.RESERVED]?.map { SeatPositionResponse(it.rowNum, it.colNum) } ?: emptyList()
        )
    }

    /**
     * 만료된 점유 좌석 정리 (스케줄러에서 호출)
     */
    @Transactional
    fun cleanupExpiredHolds(): Int {
        return seatStatusRepository.deleteExpiredHolds(LocalDateTime.now())
    }
}
