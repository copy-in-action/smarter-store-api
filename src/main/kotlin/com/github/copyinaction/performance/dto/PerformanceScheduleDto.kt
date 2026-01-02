package com.github.copyinaction.performance.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.github.copyinaction.performance.domain.PerformanceSchedule
import com.github.copyinaction.performance.domain.TicketOption
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

@Schema(description = "공연 회차 생성 요청 DTO")
data class CreatePerformanceScheduleRequest(
    @field:NotNull
    @Schema(description = "공연 날짜 및 시간", example = "2026-01-09T19:30:00", required = true)
    val showDateTime: LocalDateTime,

    @field:NotNull
    @Schema(description = "티켓 판매 시작 일시", example = "2026-01-02T10:00:00", required = true)
    val saleStartDateTime: LocalDateTime,

    @field:NotEmpty
    @field:Valid
    @Schema(description = "회차별 티켓 가격 옵션 목록", required = true)
    val ticketOptions: List<TicketOptionRequest>
)

@Schema(description = "공연 회차 응답 DTO")
data class PerformanceScheduleResponse(
    @Schema(description = "회차 ID", example = "1")
    val id: Long,

    @Schema(description = "공연 날짜 및 시간", example = "2026-01-09T19:30:00")
    val showDateTime: LocalDateTime,

    @Schema(description = "티켓 판매 시작 일시", example = "2026-01-02T10:00:00")
    val saleStartDateTime: LocalDateTime,

    @Schema(description = "생성일시")
    val createdAt: LocalDateTime?,

    @Schema(description = "수정일시")
    val updatedAt: LocalDateTime?,

    @Schema(description = "회차별 티켓 가격 옵션 목록")
    val ticketOptions: List<TicketOptionResponse>
) {
    companion object {
        fun from(performanceSchedule: PerformanceSchedule, ticketOptions: List<TicketOption>): PerformanceScheduleResponse {
            return PerformanceScheduleResponse(
                id = performanceSchedule.id,
                showDateTime = performanceSchedule.showDateTime,
                saleStartDateTime = performanceSchedule.saleStartDateTime,
                createdAt = performanceSchedule.createdAt,
                updatedAt = performanceSchedule.updatedAt,
                ticketOptions = ticketOptions.map { TicketOptionResponse.from(it) }
            )
        }
    }
}

@Schema(description = "공연 회차 수정 요청 DTO")
data class UpdatePerformanceScheduleRequest(
    @field:NotNull
    @Schema(description = "공연 날짜 및 시간", example = "2026-01-09T19:30:00", required = true)
    val showDateTime: LocalDateTime,

    @field:NotNull
    @Schema(description = "티켓 판매 시작 일시", example = "2026-01-02T10:00:00", required = true)
    val saleStartDateTime: LocalDateTime,

    @field:NotEmpty
    @field:Valid
    @Schema(description = "회차별 티켓 가격 옵션 목록", required = true)
    val ticketOptions: List<TicketOptionRequest>
)

// === 사용자용 회차 조회 DTO ===

@Schema(description = "예매 가능 회차 응답 DTO (잔여석 포함)")
data class AvailableScheduleResponse(
    @Schema(description = "회차 ID", example = "18")
    val id: Long,

    @Schema(description = "공연 날짜 및 시간", example = "2025-12-26T09:39:00")
    val showDateTime: LocalDateTime,

    @Schema(description = "등급별 잔여석 정보")
    val ticketOptions: List<TicketOptionWithRemainingSeatsResponse>
) {
    companion object {
        fun from(
            schedule: PerformanceSchedule,
            ticketOptionsWithSeats: List<TicketOptionWithRemainingSeatsResponse>
        ): AvailableScheduleResponse {
            return AvailableScheduleResponse(
                id = schedule.id,
                showDateTime = schedule.showDateTime,
                ticketOptions = ticketOptionsWithSeats
            )
        }
    }
}

@Schema(description = "등급별 잔여석 및 가격 정보 DTO")
data class TicketOptionWithRemainingSeatsResponse(
    @Schema(description = "좌석 등급", example = "R")
    val seatGrade: String,

    @Schema(description = "가격", example = "150000")
    val price: Int,

    @Schema(description = "잔여석 수", example = "10")
    val remainingSeats: Int
)

