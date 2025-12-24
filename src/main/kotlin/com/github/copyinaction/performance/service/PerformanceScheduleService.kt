package com.github.copyinaction.performance.service

import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.performance.domain.PerformanceSchedule
import com.github.copyinaction.performance.domain.TicketOption
import com.github.copyinaction.performance.dto.AvailableScheduleResponse
import com.github.copyinaction.performance.dto.CreatePerformanceScheduleRequest
import com.github.copyinaction.performance.dto.PerformanceScheduleResponse
import com.github.copyinaction.performance.dto.TicketOptionWithRemainingSeatsResponse
import com.github.copyinaction.performance.dto.UpdatePerformanceScheduleRequest
import com.github.copyinaction.performance.repository.PerformanceRepository
import com.github.copyinaction.performance.repository.PerformanceScheduleRepository
import com.github.copyinaction.performance.repository.TicketOptionRepository
import com.github.copyinaction.seat.repository.ScheduleSeatStatusRepository
import com.github.copyinaction.venue.domain.SeatGrade
import com.github.copyinaction.venue.util.SeatingChartParser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class PerformanceScheduleService(
    private val performanceRepository: PerformanceRepository,
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val ticketOptionRepository: TicketOptionRepository,
    private val scheduleSeatStatusRepository: ScheduleSeatStatusRepository,
    private val seatingChartParser: SeatingChartParser
) {

    @Transactional
    fun createSchedule(
        performanceId: Long,
        request: CreatePerformanceScheduleRequest
    ): PerformanceScheduleResponse {
        val performance = performanceRepository.findById(performanceId)
            .orElseThrow { CustomException(ErrorCode.PERFORMANCE_NOT_FOUND) }

        val performanceSchedule = PerformanceSchedule.create(
            performance = performance,
            showDateTime = request.showDateTime,
            saleStartDateTime = request.saleStartDateTime
        )
        val savedSchedule = performanceScheduleRepository.save(performanceSchedule)

        val ticketOptions = request.ticketOptions.map { ticketOptionRequest ->
            TicketOption(
                performanceSchedule = savedSchedule,
                seatGrade = ticketOptionRequest.seatGrade,
                price = ticketOptionRequest.price
            )
        }
        val savedTicketOptions = ticketOptionRepository.saveAll(ticketOptions)

        return PerformanceScheduleResponse.from(savedSchedule, savedTicketOptions)
    }

    fun getSchedule(scheduleId: Long): PerformanceScheduleResponse {
        val schedule = findScheduleById(scheduleId)
        val ticketOptions = ticketOptionRepository.findByPerformanceScheduleId(scheduleId)
        return PerformanceScheduleResponse.from(schedule, ticketOptions)
    }

    fun getAllSchedules(performanceId: Long): List<PerformanceScheduleResponse> {
        if (!performanceRepository.existsById(performanceId)) {
            throw CustomException(ErrorCode.PERFORMANCE_NOT_FOUND)
        }
        return performanceScheduleRepository.findByPerformanceId(performanceId).map { schedule ->
            val ticketOptions = ticketOptionRepository.findByPerformanceScheduleId(schedule.id)
            PerformanceScheduleResponse.from(schedule, ticketOptions)
        }
    }

    @Transactional
    fun updateSchedule(scheduleId: Long, request: UpdatePerformanceScheduleRequest): PerformanceScheduleResponse {
        val schedule = findScheduleById(scheduleId)

        schedule.update(
            showDateTime = request.showDateTime,
            saleStartDateTime = request.saleStartDateTime
        )
        val updatedSchedule = performanceScheduleRepository.save(schedule)

        // 기존 티켓 옵션 삭제 후 새로 저장
        ticketOptionRepository.deleteByPerformanceScheduleId(scheduleId)
        val newTicketOptions = request.ticketOptions.map { ticketOptionRequest ->
            TicketOption(
                performanceSchedule = updatedSchedule,
                seatGrade = ticketOptionRequest.seatGrade,
                price = ticketOptionRequest.price
            )
        }
        val savedTicketOptions = ticketOptionRepository.saveAll(newTicketOptions)

        return PerformanceScheduleResponse.from(updatedSchedule, savedTicketOptions)
    }

    @Transactional
    fun deleteSchedule(scheduleId: Long) {
        val schedule = findScheduleById(scheduleId)
        ticketOptionRepository.deleteByPerformanceScheduleId(scheduleId) // 관련 티켓 옵션 먼저 삭제
        performanceScheduleRepository.delete(schedule)
    }

    // === 사용자용 API ===

    /**
     * 예매 가능한 회차 날짜 목록 조회
     * - 티켓 판매가 시작되었고 공연이 아직 시작하지 않은 회차들의 showDateTime 반환
     */
    fun getAvailableDates(performanceId: Long): List<LocalDateTime> {
        if (!performanceRepository.existsById(performanceId)) {
            throw CustomException(ErrorCode.PERFORMANCE_NOT_FOUND)
        }

        val now = LocalDateTime.now()
        val schedules = performanceScheduleRepository.findAvailableSchedules(performanceId, now)

        return schedules.map { it.showDateTime }
    }

    /**
     * 특정 날짜의 예매 가능 회차 목록 조회 (잔여석 포함)
     * - 공연시간 내림차순 정렬
     * - 각 좌석 등급의 잔여석 수 포함
     */
    fun getAvailableSchedulesByDate(performanceId: Long, date: LocalDate): List<AvailableScheduleResponse> {
        val performance = performanceRepository.findById(performanceId)
            .orElseThrow { CustomException(ErrorCode.PERFORMANCE_NOT_FOUND) }

        val now = LocalDateTime.now()
        val dateStart = date.atStartOfDay()
        val dateEnd = date.plusDays(1).atStartOfDay()
        val schedules = performanceScheduleRepository.findAvailableSchedulesByDate(performanceId, now, dateStart, dateEnd)

        val venue = performance.venue
        val seatingChartJson = venue?.seatingChart
        val seatsByGrade = seatingChartParser.countSeatsByGrade(seatingChartJson)

        return schedules.map { schedule ->
            val ticketOptions = ticketOptionRepository.findByPerformanceScheduleId(schedule.id)
            val occupiedSeats = scheduleSeatStatusRepository.findByScheduleId(schedule.id)

            // 점유된 좌석들의 등급별 개수 계산
            val occupiedByGrade = mutableMapOf<SeatGrade, Int>()
            for (seat in occupiedSeats) {
                val grade = seatingChartParser.getSeatGrade(seatingChartJson, seat.rowNum, seat.colNum)
                if (grade != null) {
                    occupiedByGrade[grade] = (occupiedByGrade[grade] ?: 0) + 1
                }
            }

            // 등급별 잔여석 계산
            val ticketOptionsWithSeats = ticketOptions.map { option ->
                val totalSeats = seatsByGrade[option.seatGrade] ?: 0
                val occupied = occupiedByGrade[option.seatGrade] ?: 0
                val remaining = maxOf(0, totalSeats - occupied)

                TicketOptionWithRemainingSeatsResponse(
                    seatGrade = option.seatGrade.name,
                    remainingSeats = remaining
                )
            }

            AvailableScheduleResponse.from(schedule, ticketOptionsWithSeats)
        }
    }

    private fun findScheduleById(scheduleId: Long): PerformanceSchedule {
        return performanceScheduleRepository.findById(scheduleId)
            .orElseThrow { CustomException(ErrorCode.PERFORMANCE_SCHEDULE_NOT_FOUND) }
    }
}
