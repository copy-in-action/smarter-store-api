package com.github.copyinaction.performance.dto

import com.github.copyinaction.performance.domain.Performance
import com.github.copyinaction.performance.domain.PerformanceSchedule
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

@Schema(description = "공연 스케줄 정보 응답 DTO")
data class PerformanceScheduleResponse(
    @Schema(description = "공연 스케줄 ID", example = "1")
    val id: Long,

    @Schema(description = "실제 공연 날짜 및 시간", example = "2025-12-24T19:30:00")
    val showDatetime: LocalDateTime,

    @Schema(description = "티켓 판매 시작 시각", example = "2025-11-20T14:00:00")
    val saleStartDatetime: LocalDateTime?,
    
    @Schema(description = "스케줄 정보 생성일시", example = "2023-01-01T12:00:00")
    val createdAt: LocalDateTime?,

    @Schema(description = "스케줄 정보 수정일시", example = "2023-01-01T12:00:00")
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun from(schedule: PerformanceSchedule): PerformanceScheduleResponse {
            return PerformanceScheduleResponse(
                id = schedule.id,
                showDatetime = schedule.showDatetime,
                saleStartDatetime = schedule.saleStartDatetime,
                createdAt = schedule.createdAt,
                updatedAt = schedule.updatedAt
            )
        }
    }
}

@Schema(description = "공연 스케줄 생성 요청 DTO")
data class CreatePerformanceScheduleRequest(
    @field:NotNull
    @Schema(description = "생성할 공연 날짜 및 시간", example = "2025-12-24T19:30:00", required = true)
    val showDatetime: LocalDateTime,

    @Schema(description = "생성할 티켓 판매 시작 시각", example = "2025-11-20T14:00:00")
    val saleStartDatetime: LocalDateTime?
) {
    fun toEntity(performance: Performance): PerformanceSchedule {
        return PerformanceSchedule(
            performance = performance,
            showDatetime = this.showDatetime,
            saleStartDatetime = this.saleStartDatetime
        )
    }
}
