package com.github.copyinaction.dto

import com.github.copyinaction.domain.Performance
import com.github.copyinaction.domain.TicketOption
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(description = "티켓 옵션 정보 응답 DTO")
data class TicketOptionResponse(
    @Schema(description = "티켓 옵션 ID", example = "1")
    val id: Long,

    @Schema(description = "티켓/좌석 등급명", example = "R석")
    val name: String,

    @Schema(description = "가격", example = "120000.00")
    val price: BigDecimal,

    @Schema(description = "총 수량", example = "100")
    val totalQuantity: Int?,

    @Schema(description = "생성일시")
    val createdAt: LocalDateTime?,

    @Schema(description = "수정일시")
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun from(ticketOption: TicketOption): TicketOptionResponse {
            return TicketOptionResponse(
                id = ticketOption.id,
                name = ticketOption.name,
                price = ticketOption.price,
                totalQuantity = ticketOption.totalQuantity,
                createdAt = ticketOption.createdAt,
                updatedAt = ticketOption.updatedAt
            )
        }
    }
}

@Schema(description = "티켓 옵션 생성 요청 DTO")
data class CreateTicketOptionRequest(
    @field:NotBlank
    @Schema(description = "티켓/좌석 등급명", example = "R석", required = true)
    val name: String,

    @field:Positive
    @Schema(description = "가격", example = "120000.00", required = true)
    val price: BigDecimal,

    @Schema(description = "총 수량", example = "100")
    val totalQuantity: Int?
) {
    fun toEntity(performance: Performance): TicketOption {
        return TicketOption(
            performance = performance,
            name = this.name,
            price = this.price,
            totalQuantity = this.totalQuantity
        )
    }
}
