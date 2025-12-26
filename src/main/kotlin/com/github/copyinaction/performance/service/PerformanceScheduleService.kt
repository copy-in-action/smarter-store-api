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

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createSchedule(
        performanceId: Long,
        request: CreatePerformanceScheduleRequest
    ): PerformanceScheduleResponse {
        val performance = performanceRepository.findById(performanceId)
            .orElseThrow { CustomException(ErrorCode.PERFORMANCE_NOT_FOUND) }

        // Venue의 좌석배치도에서 등급별 좌석 수 계산 (Write-Time Calculation)
        val seatingChartJson = performance.venue?.seatingChart
        val seatsByGrade = seatingChartParser.countSeatsByGrade(seatingChartJson)

        // 중복 회차 검증
        if (performanceScheduleRepository.existsByPerformanceIdAndShowDateTime(performanceId, request.showDateTime)) {
            throw CustomException(ErrorCode.DUPLICATE_SCHEDULE)
        }

        // 요청된 등급이 공연장에 존재하는지 검증
        val requestedGrades = request.ticketOptions.map { it.seatGrade }.toSet()
        val availableGrades = seatsByGrade.keys
        val invalidGrades = requestedGrades - availableGrades
        if (invalidGrades.isNotEmpty()) {
            throw CustomException(ErrorCode.SEAT_GRADE_NOT_FOUND_IN_VENUE, 
                "공연장에 존재하지 않는 좌석 등급이 포함되어 있습니다: ${invalidGrades.joinToString { it.name }}")
        }

        log.info("Creating schedule for performanceId: {}. Venue ID: {}. JSON Content: {}. Parsed SeatsByGrade: {}",
            performanceId,
            performance.venue?.id,
            seatingChartJson,
            seatsByGrade
        )

        val performanceSchedule = PerformanceSchedule.create(
            performance = performance,
            showDateTime = request.showDateTime,
            saleStartDateTime = request.saleStartDateTime
        )
        val savedSchedule = performanceScheduleRepository.save(performanceSchedule)

        val ticketOptions = request.ticketOptions.map { ticketOptionRequest ->
            val totalQuantity = seatsByGrade[ticketOptionRequest.seatGrade] ?: 0
            TicketOption(
                performanceSchedule = savedSchedule,
                seatGrade = ticketOptionRequest.seatGrade,
                price = ticketOptionRequest.price,
                totalQuantity = totalQuantity
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

        // 중복 회차 검증 (자기 자신 제외)
        if (performanceScheduleRepository.existsByPerformanceIdAndShowDateTimeAndIdNot(
                schedule.performance.id,
                request.showDateTime,
                scheduleId
            )
        ) {
            throw CustomException(ErrorCode.DUPLICATE_SCHEDULE)
        }

        // Venue의 좌석배치도에서 등급별 좌석 수 계산 (Write-Time Calculation)
        val seatingChartJson = schedule.performance.venue?.seatingChart
        val seatsByGrade = seatingChartParser.countSeatsByGrade(seatingChartJson)

        // 요청된 등급이 공연장에 존재하는지 검증
        val requestedGrades = request.ticketOptions.map { it.seatGrade }.toSet()
        val availableGrades = seatsByGrade.keys
        val invalidGrades = requestedGrades - availableGrades
        if (invalidGrades.isNotEmpty()) {
            throw CustomException(ErrorCode.SEAT_GRADE_NOT_FOUND_IN_VENUE, 
                "공연장에 존재하지 않는 좌석 등급이 포함되어 있습니다: ${invalidGrades.joinToString { it.name }}")
        }

        schedule.update(
            showDateTime = request.showDateTime,
            saleStartDateTime = request.saleStartDateTime
        )
        val updatedSchedule = performanceScheduleRepository.save(schedule)

        // 기존 티켓 옵션 삭제 후 새로 저장
        ticketOptionRepository.deleteByPerformanceScheduleId(scheduleId)
        val newTicketOptions = request.ticketOptions.map { ticketOptionRequest ->
            val totalQuantity = seatsByGrade[ticketOptionRequest.seatGrade] ?: 0
            TicketOption(
                performanceSchedule = updatedSchedule,
                seatGrade = ticketOptionRequest.seatGrade,
                price = ticketOptionRequest.price,
                totalQuantity = totalQuantity
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
     * - Write-Time Calculation: totalQuantity는 TicketOption에 저장된 값 사용
     */
    fun getAvailableSchedulesByDate(performanceId: Long, date: LocalDate): List<AvailableScheduleResponse> {
        if (!performanceRepository.existsById(performanceId)) {
            throw CustomException(ErrorCode.PERFORMANCE_NOT_FOUND)
        }

        val now = LocalDateTime.now()
        val dateStart = date.atStartOfDay()
        val dateEnd = date.plusDays(1).atStartOfDay()
        val schedules = performanceScheduleRepository.findAvailableSchedulesByDate(performanceId, now, dateStart, dateEnd)

        return schedules.map { schedule ->
            val ticketOptions = ticketOptionRepository.findByPerformanceScheduleId(schedule.id)

            // 등급별 점유 좌석 수 집계 (DB에서 직접 조회)
            val occupiedByGradeRaw = scheduleSeatStatusRepository.countByScheduleIdGroupBySeatGrade(schedule.id)
            val occupiedByGrade = occupiedByGradeRaw.associate {
                (it[0] as SeatGrade) to (it[1] as Long).toInt()
            }

            // 등급별 잔여석 계산 (totalQuantity는 TicketOption에 저장된 값 사용)
            val ticketOptionsWithSeats = ticketOptions.map { option ->
                val totalSeats = option.totalQuantity
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
