package com.github.copyinaction.performance.service

import com.github.copyinaction.booking.domain.BookingStatus
import com.github.copyinaction.booking.repository.BookingRepository
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
    private val bookingRepository: BookingRepository,
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

        // 중복 회차 검증 (Entity에서 초 단위 절삭 처리)
        val truncatedShowDateTime = request.showDateTime.withSecond(0).withNano(0)
        if (performanceScheduleRepository.existsByPerformanceIdAndShowDateTime(performanceId, truncatedShowDateTime)) {
            throw CustomException(ErrorCode.DUPLICATE_SCHEDULE)
        }

        // 요청된 등급이 공연장에 존재하는지 검증
        val requestedGrades = request.ticketOptions.map { it.seatGrade }.toSet()
        seatingChartParser.validateSeatGrades(seatsByGrade.keys, requestedGrades)

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

        request.ticketOptions.forEach { ticketOptionRequest ->
            val totalQuantity = seatsByGrade[ticketOptionRequest.seatGrade] ?: 0
            val ticketOption = TicketOption(
                performanceSchedule = performanceSchedule,
                seatGrade = ticketOptionRequest.seatGrade,
                price = ticketOptionRequest.price,
                totalQuantity = totalQuantity
            )
            performanceSchedule.addTicketOption(ticketOption)
        }

        val savedSchedule = performanceScheduleRepository.save(performanceSchedule)

        return PerformanceScheduleResponse.from(savedSchedule, savedSchedule.ticketOptions)
    }

    fun getSchedule(scheduleId: Long): PerformanceScheduleResponse {
        val schedule = findScheduleById(scheduleId)
        return PerformanceScheduleResponse.from(schedule, schedule.ticketOptions)
    }

    fun getAllSchedules(performanceId: Long): List<PerformanceScheduleResponse> {
        if (!performanceRepository.existsById(performanceId)) {
            throw CustomException(ErrorCode.PERFORMANCE_NOT_FOUND)
        }
        return performanceScheduleRepository.findByPerformanceId(performanceId).map { schedule ->
            PerformanceScheduleResponse.from(schedule, schedule.ticketOptions)
        }
    }

    @Transactional
    fun updateSchedule(scheduleId: Long, request: UpdatePerformanceScheduleRequest): PerformanceScheduleResponse {
        val schedule = findScheduleById(scheduleId)

        // 예매가 진행된 경우 수정 불가 (PENDING, CONFIRMED 상태 체크)
        if (bookingRepository.existsBySchedule_IdAndStatusIn(
                scheduleId,
                listOf(BookingStatus.PENDING, BookingStatus.CONFIRMED)
            )
        ) {
            throw CustomException(ErrorCode.SCHEDULE_ALREADY_BOOKED)
        }

        // 중복 회차 검증 (자기 자신 제외, Entity에서 초 단위 절삭 처리)
        val truncatedShowDateTime = request.showDateTime.withSecond(0).withNano(0)
        if (performanceScheduleRepository.existsByPerformanceIdAndShowDateTimeAndIdNot(
                schedule.performance.id,
                truncatedShowDateTime,
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
        seatingChartParser.validateSeatGrades(seatsByGrade.keys, requestedGrades)

        schedule.update(
            showDateTime = request.showDateTime,
            saleStartDateTime = request.saleStartDateTime
        )

        // 기존 티켓 옵션 삭제 후 새로 추가 (OrphanRemoval + Cascade)
        schedule.clearTicketOptions()
        request.ticketOptions.forEach { ticketOptionRequest ->
            val totalQuantity = seatsByGrade[ticketOptionRequest.seatGrade] ?: 0
            val ticketOption = TicketOption(
                performanceSchedule = schedule,
                seatGrade = ticketOptionRequest.seatGrade,
                price = ticketOptionRequest.price,
                totalQuantity = totalQuantity
            )
            schedule.addTicketOption(ticketOption)
        }
        
        // 명시적 저장 및 Flush (ID 생성 보장)
        val updatedSchedule = performanceScheduleRepository.saveAndFlush(schedule)
        return PerformanceScheduleResponse.from(updatedSchedule, updatedSchedule.ticketOptions)
    }

    @Transactional
    fun deleteSchedule(scheduleId: Long) {
        val schedule = findScheduleById(scheduleId)

        // 예매가 진행된 경우 삭제 불가 (PENDING, CONFIRMED 상태 체크)
        if (bookingRepository.existsBySchedule_IdAndStatusIn(
                scheduleId,
                listOf(BookingStatus.PENDING, BookingStatus.CONFIRMED)
            )
        ) {
            throw CustomException(ErrorCode.SCHEDULE_ALREADY_BOOKED)
        }

        performanceScheduleRepository.delete(schedule) // Cascade 삭제
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
        if (!performanceRepository.existsById(performanceId)) {
            throw CustomException(ErrorCode.PERFORMANCE_NOT_FOUND)
        }

        val now = LocalDateTime.now()
        val dateStart = date.atStartOfDay()
        val dateEnd = date.plusDays(1).atStartOfDay()
        val schedules = performanceScheduleRepository.findAvailableSchedulesByDate(performanceId, now, dateStart, dateEnd)

        return schedules.map { schedule ->
            buildScheduleResponseWithRemainingSeats(schedule)
        }
    }

    /**
     * 단일 회차 조회 (잔여석 포함) - 사용자용
     */
    fun getScheduleWithRemainingSeats(scheduleId: Long): AvailableScheduleResponse {
        val schedule = findScheduleById(scheduleId)
        return buildScheduleResponseWithRemainingSeats(schedule)
    }

    /**
     * 회차 응답 DTO 생성 (잔여석 계산 포함)
     */
    private fun buildScheduleResponseWithRemainingSeats(schedule: PerformanceSchedule): AvailableScheduleResponse {
        val ticketOptions = ticketOptionRepository.findByPerformanceScheduleId(schedule.id)
        val ticketOptionsWithSeats = calculateRemainingSeats(schedule.id, ticketOptions)
        return AvailableScheduleResponse.from(schedule, ticketOptionsWithSeats)
    }

    /**
     * 등급별 잔여석 계산
     */
    private fun calculateRemainingSeats(
        scheduleId: Long,
        ticketOptions: List<TicketOption>
    ): List<TicketOptionWithRemainingSeatsResponse> {
        val occupiedByGradeRaw = scheduleSeatStatusRepository.countByScheduleIdGroupBySeatGrade(scheduleId)
        val occupiedByGrade = occupiedByGradeRaw.associate {
            (it[0] as SeatGrade) to (it[1] as Long).toInt()
        }

        return ticketOptions.map { option ->
            val totalSeats = option.totalQuantity
            val occupied = occupiedByGrade[option.seatGrade] ?: 0
            val remaining = maxOf(0, totalSeats - occupied)

            TicketOptionWithRemainingSeatsResponse(
                seatGrade = option.seatGrade.name,
                price = option.price,
                remainingSeats = remaining
            )
        }
    }

    private fun findScheduleById(scheduleId: Long): PerformanceSchedule {
        return performanceScheduleRepository.findById(scheduleId)
            .orElseThrow { CustomException(ErrorCode.PERFORMANCE_SCHEDULE_NOT_FOUND) }
    }
}
