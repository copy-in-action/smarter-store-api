package com.github.copyinaction.booking.dto

import com.github.copyinaction.booking.domain.BookingSeat
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "예매 좌석 응답")
data class BookingSeatResponse(
    @Schema(description = "예매 좌석 ID", example = "1")
    val id: Long,

    @Schema(description = "좌석 구역", example = "A")
    val section: String,

    @Schema(description = "행 번호", example = "10")
    val row: Int,

    @Schema(description = "열 번호", example = "5")
    val col: Int
) {
    companion object {
        fun from(bookingSeat: BookingSeat): BookingSeatResponse {
            return BookingSeatResponse(
                id = bookingSeat.id,
                section = bookingSeat.section,
                row = bookingSeat.row,
                col = bookingSeat.col
            )
        }
    }
}
