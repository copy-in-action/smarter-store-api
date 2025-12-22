package com.github.copyinaction.booking.dto

import com.github.copyinaction.booking.domain.BookingSeat
import com.github.copyinaction.venue.domain.SeatGrade
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "예매 좌석 응답")
data class BookingSeatResponse(
    @Schema(description = "예매 좌석 ID", example = "1")
    val id: Long,

    @Schema(description = "좌석 구역", example = "A")
    val section: String,

    @Schema(description = "좌석 열", example = "10")
    val rowName: String,

    @Schema(description = "좌석 번호", example = "5")
    val seatNumber: Int,

    @Schema(description = "좌석 등급", example = "VIP")
    val grade: SeatGrade,

    @Schema(description = "좌석 가격", example = "100000")
    val price: Int
) {
    companion object {
        fun from(bookingSeat: BookingSeat): BookingSeatResponse {
            return BookingSeatResponse(
                id = bookingSeat.id,
                section = bookingSeat.section,
                rowName = bookingSeat.rowName,
                seatNumber = bookingSeat.seatNumber,
                grade = bookingSeat.grade,
                price = bookingSeat.price
            )
        }
    }
}
