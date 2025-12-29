package com.github.copyinaction.booking.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 좌석 위치 요청 (예매용)
 */
@Schema(description = "좌석 위치")
data class SeatPositionRequest(
    @field:NotNull(message = "행 번호는 필수입니다.")
    @field:Min(value = 1, message = "행 번호는 1 이상이어야 합니다.")
    @Schema(description = "행 번호 (1부터 시작)", example = "1")
    val row: Int,

    @field:NotNull(message = "열 번호는 필수입니다.")
    @field:Min(value = 1, message = "열 번호는 1 이상이어야 합니다.")
    @Schema(description = "열 번호 (1부터 시작)", example = "1")
    val col: Int
)

@Schema(description = "예매 시작 요청 - 좌석 일괄 점유")
data class StartBookingRequest(
    @field:NotNull(message = "scheduleId는 필수입니다.")
    @Schema(description = "공연 회차 ID", example = "1")
    val scheduleId: Long,

    @field:NotEmpty(message = "좌석을 1개 이상 선택해야 합니다.")
    @field:Size(max = 4, message = "최대 4석까지 선택 가능합니다.")
    @field:Valid
    @Schema(description = "선택한 좌석 목록 (최대 4석)")
    val seats: List<SeatPositionRequest>
)
