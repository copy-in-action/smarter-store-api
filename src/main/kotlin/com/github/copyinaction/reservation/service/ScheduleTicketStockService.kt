package com.github.copyinaction.reservation.service

import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.performance.repository.PerformanceScheduleRepository
import com.github.copyinaction.performance.repository.TicketOptionRepository
import com.github.copyinaction.reservation.domain.ScheduleTicketStock
import com.github.copyinaction.reservation.dto.CreateScheduleTicketStockRequest
import com.github.copyinaction.reservation.dto.ScheduleTicketStockResponse
import com.github.copyinaction.reservation.dto.UpdateScheduleTicketStockRequest
import com.github.copyinaction.reservation.repository.ScheduleTicketStockRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ScheduleTicketStockService(
    private val scheduleTicketStockRepository: ScheduleTicketStockRepository,
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val ticketOptionRepository: TicketOptionRepository
) {

    @Transactional
    fun createStock(request: CreateScheduleTicketStockRequest): ScheduleTicketStockResponse {
        val schedule = performanceScheduleRepository.findById(request.scheduleId)
            .orElseThrow { CustomException(ErrorCode.PERFORMANCE_SCHEDULE_NOT_FOUND) }

        val ticketOption = ticketOptionRepository.findById(request.ticketOptionId)
            .orElseThrow { CustomException(ErrorCode.TICKET_OPTION_NOT_FOUND) }

        // 중복 검사
        val existing = scheduleTicketStockRepository.findByScheduleIdAndTicketOptionId(
            request.scheduleId, request.ticketOptionId
        )
        if (existing != null) {
            throw CustomException(ErrorCode.SCHEDULE_TICKET_STOCK_ALREADY_EXISTS)
        }

        val stock = ScheduleTicketStock(
            schedule = schedule,
            ticketOption = ticketOption,
            totalQuantity = request.totalQuantity,
            remainingQuantity = request.totalQuantity
        )

        val savedStock = scheduleTicketStockRepository.save(stock)
        return ScheduleTicketStockResponse.from(savedStock)
    }

    @Transactional
    fun updateStock(stockId: Long, request: UpdateScheduleTicketStockRequest): ScheduleTicketStockResponse {
        val stock = findStockById(stockId)

        require(request.remainingQuantity <= request.totalQuantity) {
            "잔여 좌석 수는 총 좌석 수를 초과할 수 없습니다."
        }

        stock.totalQuantity = request.totalQuantity
        stock.remainingQuantity = request.remainingQuantity

        return ScheduleTicketStockResponse.from(stock)
    }

    fun getStock(stockId: Long): ScheduleTicketStockResponse {
        val stock = findStockById(stockId)
        return ScheduleTicketStockResponse.from(stock)
    }

    fun getStocksByScheduleId(scheduleId: Long): List<ScheduleTicketStockResponse> {
        return scheduleTicketStockRepository.findByScheduleIdWithDetails(scheduleId)
            .map { ScheduleTicketStockResponse.from(it) }
    }

    fun getStocksByPerformanceId(performanceId: Long): List<ScheduleTicketStockResponse> {
        return scheduleTicketStockRepository.findByPerformanceIdWithDetails(performanceId)
            .map { ScheduleTicketStockResponse.from(it) }
    }

    @Transactional
    fun deleteStock(stockId: Long) {
        val stock = findStockById(stockId)
        scheduleTicketStockRepository.delete(stock)
    }

    private fun findStockById(id: Long): ScheduleTicketStock {
        return scheduleTicketStockRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.SCHEDULE_TICKET_STOCK_NOT_FOUND) }
    }
}
