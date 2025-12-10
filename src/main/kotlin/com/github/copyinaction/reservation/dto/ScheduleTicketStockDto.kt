package com.github.copyinaction.reservation.dto

import com.github.copyinaction.reservation.domain.ScheduleTicketStock
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(description = "회차별 좌석등급 재고 응답 DTO")
data class ScheduleTicketStockResponse(
    @Schema(description = "재고 ID", example = "1")
    val id: Long,

    @Schema(description = "공연 회차 ID", example = "1")
    val scheduleId: Long,

    @Schema(description = "공연 일시", example = "2025-12-25T14:00:00")
    val showDatetime: LocalDateTime,

    @Schema(description = "좌석 등급 ID", example = "1")
    val ticketOptionId: Long,

    @Schema(description = "좌석 등급명", example = "VIP석")
    val ticketOptionName: String,

    @Schema(description = "좌석 가격", example = "150000")
    val price: BigDecimal,

    @Schema(description = "총 좌석 수", example = "100")
    val totalQuantity: Int,

    @Schema(description = "잔여 좌석 수", example = "87")
    val remainingQuantity: Int,

    @Schema(description = "매진 여부", example = "false")
    val soldOut: Boolean
) {
    companion object {
        fun from(stock: ScheduleTicketStock): ScheduleTicketStockResponse {
            return ScheduleTicketStockResponse(
                id = stock.id,
                scheduleId = stock.schedule.id,
                showDatetime = stock.schedule.showDatetime,
                ticketOptionId = stock.ticketOption.id,
                ticketOptionName = stock.ticketOption.name,
                price = stock.ticketOption.price,
                totalQuantity = stock.totalQuantity,
                remainingQuantity = stock.remainingQuantity,
                soldOut = stock.isSoldOut()
            )
        }
    }
}

@Schema(description = "회차별 좌석등급 재고 생성 요청 DTO")
data class CreateScheduleTicketStockRequest(
    @field:NotNull
    @Schema(description = "공연 회차 ID", example = "1", required = true)
    val scheduleId: Long,

    @field:NotNull
    @Schema(description = "좌석 등급 ID", example = "1", required = true)
    val ticketOptionId: Long,

    @field:NotNull
    @field:Min(0)
    @Schema(description = "총 좌석 수", example = "100", required = true)
    val totalQuantity: Int
)

@Schema(description = "회차별 좌석등급 재고 수정 요청 DTO")
data class UpdateScheduleTicketStockRequest(
    @field:NotNull
    @field:Min(0)
    @Schema(description = "총 좌석 수", example = "100", required = true)
    val totalQuantity: Int,

    @field:NotNull
    @field:Min(0)
    @Schema(description = "잔여 좌석 수", example = "87", required = true)
    val remainingQuantity: Int
)
