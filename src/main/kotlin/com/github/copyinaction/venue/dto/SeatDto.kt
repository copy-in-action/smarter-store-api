package com.github.copyinaction.venue.dto

import com.github.copyinaction.venue.domain.Seat
import com.github.copyinaction.venue.domain.SeatGrade
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(description = "좌석 정보 응답 DTO")
data class SeatResponse(
    @Schema(description = "좌석 ID", example = "1")
    val id: Long,

    @Schema(description = "공연장 ID", example = "1")
    val venueId: Long,

    @Schema(description = "구역", example = "VIP구역")
    val section: String,

    @Schema(description = "좌석 열", example = "A")
    val row: String,

    @Schema(description = "좌석 번호", example = "1")
    val number: Int,

    @Schema(description = "좌석 등급", example = "VIP")
    val seatGrade: SeatGrade,

    @Schema(description = "표시 이름", example = "VIP구역 A-1")
    val displayName: String,

    @Schema(description = "좌석배치도 X좌표", example = "100")
    val positionX: Int,

    @Schema(description = "좌석배치도 Y좌표", example = "50")
    val positionY: Int,

    @Schema(description = "생성일시")
    val createdAt: LocalDateTime?,

    @Schema(description = "수정일시")
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun from(seat: Seat): SeatResponse {
            return SeatResponse(
                id = seat.id,
                venueId = seat.venue.id,
                section = seat.section,
                row = seat.row,
                number = seat.number,
                seatGrade = seat.seatGrade,
                displayName = seat.getDisplayName(),
                positionX = seat.positionX,
                positionY = seat.positionY,
                createdAt = seat.createdAt,
                updatedAt = seat.updatedAt
            )
        }
    }
}

@Schema(description = "공연장 좌석 목록 응답 DTO (좌석배치도용)")
data class VenueSeatMapResponse(
    @Schema(description = "공연장 ID")
    val venueId: Long,

    @Schema(description = "공연장 이름")
    val venueName: String,

    @Schema(description = "총 좌석 수")
    val totalSeats: Int,

    @Schema(description = "구역별 좌석 목록")
    val sections: List<SectionSeatsResponse>
)

@Schema(description = "구역별 좌석 목록")
data class SectionSeatsResponse(
    @Schema(description = "구역명")
    val name: String,

    @Schema(description = "좌석 목록")
    val seats: List<SeatResponse>
)

@Schema(description = "좌석 생성 요청 DTO")
data class CreateSeatRequest(
    @field:NotBlank(message = "구역은 비워둘 수 없습니다.")
    @field:Size(max = 50, message = "구역은 50자를 초과할 수 없습니다.")
    @Schema(description = "구역", example = "VIP구역", required = true)
    val section: String,

    @field:NotBlank(message = "좌석 열은 비워둘 수 없습니다.")
    @field:Size(max = 10, message = "좌석 열은 10자를 초과할 수 없습니다.")
    @Schema(description = "좌석 열", example = "A", required = true)
    val row: String,

    @field:Min(value = 1, message = "좌석 번호는 1 이상이어야 합니다.")
    @Schema(description = "좌석 번호", example = "1", required = true)
    val number: Int,

    @Schema(description = "좌석 등급", example = "VIP")
    val seatGrade: SeatGrade = SeatGrade.STANDARD,

    @field:Min(value = 0, message = "X좌표는 0 이상이어야 합니다.")
    @Schema(description = "좌석배치도 X좌표", example = "100", required = true)
    val positionX: Int,

    @field:Min(value = 0, message = "Y좌표는 0 이상이어야 합니다.")
    @Schema(description = "좌석배치도 Y좌표", example = "50", required = true)
    val positionY: Int
)

@Schema(description = "좌석 일괄 생성 요청 DTO")
data class BulkCreateSeatRequest(
    @field:NotEmpty(message = "좌석 목록은 비워둘 수 없습니다.")
    @field:Valid
    @Schema(description = "생성할 좌석 목록")
    val seats: List<CreateSeatRequest>
)

@Schema(description = "좌석 수정 요청 DTO")
data class UpdateSeatRequest(
    @field:NotBlank(message = "구역은 비워둘 수 없습니다.")
    @field:Size(max = 50, message = "구역은 50자를 초과할 수 없습니다.")
    @Schema(description = "구역", example = "VIP구역", required = true)
    val section: String,

    @field:NotBlank(message = "좌석 열은 비워둘 수 없습니다.")
    @field:Size(max = 10, message = "좌석 열은 10자를 초과할 수 없습니다.")
    @Schema(description = "좌석 열", example = "A", required = true)
    val row: String,

    @field:Min(value = 1, message = "좌석 번호는 1 이상이어야 합니다.")
    @Schema(description = "좌석 번호", example = "1", required = true)
    val number: Int,

    @Schema(description = "좌석 등급", example = "VIP")
    val seatGrade: SeatGrade = SeatGrade.STANDARD,

    @field:Min(value = 0, message = "X좌표는 0 이상이어야 합니다.")
    @Schema(description = "좌석배치도 X좌표", example = "100", required = true)
    val positionX: Int,

    @field:Min(value = 0, message = "Y좌표는 0 이상이어야 합니다.")
    @Schema(description = "좌석배치도 Y좌표", example = "50", required = true)
    val positionY: Int
)

@Schema(description = "좌석 일괄 생성 결과 응답 DTO")
data class BulkCreateSeatResponse(
    @Schema(description = "생성된 좌석 수")
    val createdCount: Int,

    @Schema(description = "생성된 좌석 목록")
    val seats: List<SeatResponse>
)
