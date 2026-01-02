package com.github.copyinaction.seat.service

import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.performance.domain.PerformanceSchedule
import com.github.copyinaction.seat.domain.ScheduleSeatStatus
import com.github.copyinaction.seat.domain.SeatChangeResult
import com.github.copyinaction.seat.domain.SeatPosition
import com.github.copyinaction.seat.domain.SeatStatus
import com.github.copyinaction.seat.repository.ScheduleSeatStatusRepository
import com.github.copyinaction.venue.domain.SeatGrade
import com.github.copyinaction.venue.util.SeatingChartParser
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 좌석 점유/해제 도메인 서비스
 */
@Service
class SeatOccupationService(
    private val scheduleSeatStatusRepository: ScheduleSeatStatusRepository,
    private val seatingChartParser: SeatingChartParser,
    private val sseService: SseService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 좌석 상태 DB 반영 (점유/해제/연장)
     */
    @Transactional
    fun processSeatChanges(
        schedule: PerformanceSchedule,
        userId: Long,
        changes: SeatChangeResult
    ) {
        val scheduleId = schedule.id
        try {
            // 해제될 좌석 삭제
            releaseSeats(scheduleId, userId, changes.released)

            // 유지될 좌석 만료시간 연장
            extendSeats(scheduleId, changes.kept)

            // 새로 추가될 좌석 점유
            occupySeats(schedule, userId, changes.added)

            scheduleSeatStatusRepository.flush()
        } catch (e: DataIntegrityViolationException) {
            log.warn("좌석 점유 충돌 - scheduleId: {}, userId: {}", scheduleId, userId)
            throw CustomException(ErrorCode.SEAT_ALREADY_OCCUPIED, "선택한 좌석 중 일부가 다른 사용자에게 점유되었습니다.")
        }
    }

    /**
     * SSE 이벤트 발행
     */
    fun publishSeatEvents(scheduleId: Long, changes: SeatChangeResult) {
        if (changes.released.isNotEmpty()) {
            sseService.sendReleased(scheduleId, changes.released.toList())
        }
        if (changes.added.isNotEmpty()) {
            sseService.sendOccupied(scheduleId, changes.added.toList())
        }
    }

    /**
     * 예매 확정 시 좌석 상태를 RESERVED로 변경
     */
    @Transactional
    fun confirmSeats(scheduleId: Long, seats: List<SeatPosition>, seatGrade: SeatGrade, schedule: PerformanceSchedule) {
        seats.forEach { seat ->
            val existingSeat = scheduleSeatStatusRepository.findByScheduleIdAndRowNumAndColNum(
                scheduleId, seat.row, seat.col
            )

            if (existingSeat != null) {
                if (existingSeat.seatStatus == SeatStatus.RESERVED) {
                    throw CustomException(ErrorCode.SEAT_ALREADY_OCCUPIED, "결제 도중 이미 판매 완료된 좌석이 발생했습니다.")
                }
                existingSeat.seatStatus = SeatStatus.RESERVED
                scheduleSeatStatusRepository.save(existingSeat)
            } else {
                val newSeat = ScheduleSeatStatus(
                    schedule = schedule,
                    rowNum = seat.row,
                    colNum = seat.col,
                    seatGrade = seatGrade,
                    seatStatus = SeatStatus.RESERVED
                )
                scheduleSeatStatusRepository.save(newSeat)
            }
        }

        sseService.sendConfirmed(scheduleId, seats)
    }

    /**
     * 예매 취소 시 PENDING 좌석 해제
     */
    @Transactional
    fun releaseUserPendingSeats(scheduleId: Long, userId: Long, seats: List<SeatPosition>) {
        scheduleSeatStatusRepository.deleteByScheduleIdAndHeldByAndSeatStatus(
            scheduleId, userId, SeatStatus.PENDING
        )

        if (seats.isNotEmpty()) {
            sseService.sendReleased(scheduleId, seats)
        }
    }

    private fun releaseSeats(scheduleId: Long, userId: Long, seats: Set<SeatPosition>) {
        for (seat in seats) {
            val existingSeat = scheduleSeatStatusRepository.findByScheduleIdAndRowNumAndColNum(
                scheduleId, seat.row, seat.col
            )
            if (existingSeat != null && existingSeat.heldBy == userId) {
                scheduleSeatStatusRepository.delete(existingSeat)
            }
        }
    }

    private fun extendSeats(scheduleId: Long, seats: Set<SeatPosition>) {
        for (seat in seats) {
            scheduleSeatStatusRepository.findByScheduleIdAndRowNumAndColNum(
                scheduleId, seat.row, seat.col
            )?.extendHold()
        }
    }

    private fun occupySeats(schedule: PerformanceSchedule, userId: Long, seats: Set<SeatPosition>) {
        val seatingChartJson = schedule.performance.venue?.seatingChart

        for (seat in seats) {
            val existingSeat = scheduleSeatStatusRepository.findByScheduleIdAndRowNumAndColNum(
                schedule.id, seat.row, seat.col
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
    }
}

