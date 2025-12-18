package com.github.copyinaction.performance.service

import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.performance.domain.PerformanceSchedule
import com.github.copyinaction.performance.domain.TicketOption
import com.github.copyinaction.performance.dto.CreatePerformanceScheduleRequest
import com.github.copyinaction.performance.dto.PerformanceScheduleResponse
import com.github.copyinaction.performance.dto.UpdatePerformanceScheduleRequest // 추가
import com.github.copyinaction.performance.repository.PerformanceRepository
import com.github.copyinaction.performance.repository.PerformanceScheduleRepository
import com.github.copyinaction.performance.repository.TicketOptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PerformanceScheduleService(
    private val performanceRepository: PerformanceRepository,
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val ticketOptionRepository: TicketOptionRepository
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
        val performance = performanceRepository.findById(request.performanceId)
            .orElseThrow { CustomException(ErrorCode.PERFORMANCE_NOT_FOUND) }

        // 공연 정보는 변경 불가 (회차는 특정 공연에 귀속)
        if (schedule.performance.id != performance.id) {
            throw CustomException(ErrorCode.PERFORMANCE_SCHEDULE_PERFORMANCE_CANNOT_CHANGE)
        }

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

    private fun findScheduleById(scheduleId: Long): PerformanceSchedule {
        return performanceScheduleRepository.findById(scheduleId)
            .orElseThrow { CustomException(ErrorCode.PERFORMANCE_SCHEDULE_NOT_FOUND) }
    }
}
