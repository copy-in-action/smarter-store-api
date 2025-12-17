package com.github.copyinaction.venue.dto

import com.github.copyinaction.venue.domain.SeatGrade
import com.github.copyinaction.venue.domain.VenueSeatCapacity
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

@Schema(description = "공연장 등급별 좌석 용량 응답 DTO")
data class VenueSeatCapacityResponse(
    @Schema(description = "좌석 용량 ID", example = "1")
    val id: Long,

    @Schema(description = "공연장 ID", example = "1")
    val venueId: Long,

    @Schema(description = "좌석 등급", example = "VIP")
    val seatGrade: SeatGrade,

    @Schema(description = "허용 좌석 수", example = "100")
    val capacity: Int
) {
    companion object {
        fun from(entity: VenueSeatCapacity): VenueSeatCapacityResponse {
            return VenueSeatCapacityResponse(
                id = entity.id,
                venueId = entity.venue.id,
                seatGrade = entity.seatGrade,
                capacity = entity.capacity
            )
        }
    }
}

@Schema(description = "공연장 등급별 좌석 용량 생성/수정 요청 DTO")
data class VenueSeatCapacityRequest(
    @field:NotNull(message = "좌석 등급은 필수입니다.")
    @Schema(description = "좌석 등급", example = "VIP", required = true)
    val seatGrade: SeatGrade,

    @field:NotNull(message = "좌석 수는 필수입니다.")
    @field:Min(value = 0, message = "좌석 수는 0 이상이어야 합니다.")
    @Schema(description = "허용 좌석 수", example = "100", required = true)
    val capacity: Int
)

@Schema(description = "공연장 등급별 좌석 용량 일괄 설정 요청 DTO")
data class VenueSeatCapacityBulkRequest(
    @field:NotNull(message = "좌석 용량 목록은 필수입니다.")
    @Schema(description = "좌석 용량 목록")
    val capacities: List<VenueSeatCapacityRequest>
)
