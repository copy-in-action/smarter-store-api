package com.github.copyinaction.booking.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

@Schema(description = "좌석 선택/취소 요청")
data class SeatRequest(
    @field:NotBlank(message = "section은 필수입니다.")
    @Schema(description = "좌석 구역", example = "A")
    val section: String,

    @field:NotBlank(message = "rowName은 필수입니다.")
    @Schema(description = "좌석 열", example = "10")
    val rowName: String,

    @field:NotNull(message = "seatNumber는 필수입니다.")
    @field:Min(value = 1, message = "seatNumber는 1 이상이어야 합니다.")
    @Schema(description = "좌석 번호", example = "5")
    val seatNumber: Int
)
