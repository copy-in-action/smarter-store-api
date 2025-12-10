package com.github.copyinaction.reservation.service

import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.performance.domain.PerformanceSchedule
import com.github.copyinaction.performance.domain.TicketOption
import com.github.copyinaction.performance.repository.PerformanceScheduleRepository
import com.github.copyinaction.performance.repository.TicketOptionRepository
import com.github.copyinaction.reservation.domain.ScheduleSeat
import com.github.copyinaction.reservation.domain.SeatStatus
import com.github.copyinaction.reservation.dto.*
import com.github.copyinaction.reservation.repository.ScheduleSeatRepository
import com.github.copyinaction.venue.domain.SeatGrade
import com.github.copyinaction.venue.repository.SeatRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ScheduleSeatService(
    private val scheduleSeatRepository: ScheduleSeatRepository,
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val seatRepository: SeatRepository,
    private val ticketOptionRepository: TicketOptionRepository
) {

    @Transactional
    fun initializeScheduleSeats(scheduleId: Long, request: InitializeScheduleSeatsRequest): ScheduleSeatMapResponse {
        val schedule = findScheduleById(scheduleId)

        // 이미 초기화된 경우 예외
        if (scheduleSeatRepository.existsByScheduleId(scheduleId)) {
            throw CustomException(ErrorCode.SCHEDULE_SEATS_ALREADY_INITIALIZED)
        }

        val performance = schedule.performance
        val venue = performance.venue
            ?: throw CustomException(ErrorCode.VENUE_NOT_FOUND)

        // 공연장의 모든 좌석 조회
        val seats = seatRepository.findByVenueIdOrderBySectionAndRowAndNumber(venue.id)
        if (seats.isEmpty()) {
            throw CustomException(ErrorCode.SEAT_NOT_FOUND)
        }

        // 티켓 옵션 조회 및 매핑 검증
        val ticketOptionMap = mutableMapOf<SeatGrade, TicketOption>()
        for ((grade, ticketOptionId) in request.seatGradeMapping) {
            val ticketOption = ticketOptionRepository.findById(ticketOptionId)
                .orElseThrow { CustomException(ErrorCode.TICKET_OPTION_NOT_FOUND) }
            ticketOptionMap[grade] = ticketOption
        }

        // 회차별 좌석 생성
        val scheduleSeats = seats.map { seat ->
            val ticketOption = ticketOptionMap[seat.seatGrade]
                ?: ticketOptionMap[SeatGrade.STANDARD]
                ?: throw CustomException(ErrorCode.TICKET_OPTION_NOT_FOUND)

            ScheduleSeat(
                schedule = schedule,
                seat = seat,
                ticketOption = ticketOption,
                status = SeatStatus.AVAILABLE
            )
        }

        scheduleSeatRepository.saveAll(scheduleSeats)

        return getScheduleSeats(scheduleId, null, null)
    }

    fun getScheduleSeats(scheduleId: Long, userId: Long?, sessionId: String?): ScheduleSeatMapResponse {
        val schedule = findScheduleById(scheduleId)
        val performance = schedule.performance
        val venue = performance.venue
            ?: throw CustomException(ErrorCode.VENUE_NOT_FOUND)

        val scheduleSeats = scheduleSeatRepository.findByScheduleIdWithSeatAndTicketOption(scheduleId)

        // 구역별로 그룹핑
        val sectionMap = scheduleSeats.groupBy { it.seat.section }
        val sections = sectionMap.map { (sectionName, sectionSeats) ->
            ScheduleSectionSeatsResponse(
                name = sectionName,
                seats = sectionSeats.map { ScheduleSeatResponse.from(it, userId, sessionId) }
            )
        }

        // 상태별 좌석 수 계산
        val summary = SeatStatusSummary(
            total = scheduleSeats.size,
            available = scheduleSeats.count { it.status == SeatStatus.AVAILABLE },
            held = scheduleSeats.count { it.status == SeatStatus.HELD },
            reserved = scheduleSeats.count { it.status == SeatStatus.RESERVED }
        )

        return ScheduleSeatMapResponse(
            scheduleId = schedule.id,
            performanceTitle = performance.title,
            showDatetime = schedule.showDatetime,
            venueName = venue.name,
            sections = sections,
            summary = summary
        )
    }

    fun getSeatStatusSummary(scheduleId: Long): SeatStatusSummary {
        findScheduleById(scheduleId)

        val statusCounts = scheduleSeatRepository.countByScheduleIdGroupByStatus(scheduleId)
        var total = 0
        var available = 0
        var held = 0
        var reserved = 0

        for (result in statusCounts) {
            val status = result[0] as SeatStatus
            val count = (result[1] as Long).toInt()
            total += count
            when (status) {
                SeatStatus.AVAILABLE -> available = count
                SeatStatus.HELD -> held = count
                SeatStatus.RESERVED -> reserved = count
                SeatStatus.UNAVAILABLE -> {} // Or handle as needed
                else -> {}
            }
        }

        return SeatStatusSummary(
            total = total,
            available = available,
            held = held,
            reserved = reserved
        )
    }

    @Transactional
    fun reinitializeScheduleSeats(scheduleId: Long, request: InitializeScheduleSeatsRequest): ScheduleSeatMapResponse {
        // 기존 좌석 삭제
        scheduleSeatRepository.deleteByScheduleId(scheduleId)

        // 새로 초기화
        return initializeScheduleSeats(scheduleId, request)
    }

    private fun findScheduleById(id: Long): PerformanceSchedule {
        return performanceScheduleRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.PERFORMANCE_SCHEDULE_NOT_FOUND) }
    }
}
