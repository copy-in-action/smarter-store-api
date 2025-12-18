package com.github.copyinaction.performance.service

import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.performance.domain.PerformanceSchedule
import com.github.copyinaction.performance.domain.TicketOption
import com.github.copyinaction.performance.dto.CreatePerformanceScheduleRequest
import com.github.copyinaction.performance.dto.PerformanceScheduleResponse
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
}
