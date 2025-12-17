package com.github.copyinaction.performance.dto

import com.github.copyinaction.performance.domain.TicketOption
import com.github.copyinaction.venue.domain.SeatGrade
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "티켓 옵션 생성/수정 요청 DTO")
data class TicketOptionRequest(
    @Schema(description = "좌석 등급", example = "VIP", required = true)
    val seatGrade: SeatGrade,

    @Schema(description = "가격", example = "150000", required = true)
    val price: Int
)

@Schema(description = "티켓 옵션 응답 DTO")
data class TicketOptionResponse(
    @Schema(description = "티켓 옵션 ID", example = "1")
    val id: Long,

    @Schema(description = "좌석 등급", example = "VIP")
    val seatGrade: SeatGrade,

    @Schema(description = "가격", example = "150000")
    val price: Int
) {
    companion object {
        fun from(ticketOption: TicketOption): TicketOptionResponse {
            return TicketOptionResponse(
                id = ticketOption.id,
                seatGrade = ticketOption.seatGrade,
                price = ticketOption.price
            )
        }
    }
}
