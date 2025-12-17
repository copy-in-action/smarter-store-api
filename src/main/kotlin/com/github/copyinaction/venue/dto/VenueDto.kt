package com.github.copyinaction.venue.dto

import com.github.copyinaction.venue.domain.Venue
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(description = "공연장 정보 응답 DTO")
data class VenueResponse(
    @Schema(description = "공연장 ID", example = "1")
    val id: Long,

    @Schema(description = "공연장 이름", example = "올림픽공원 올림픽홀")
    val name: String,

    @Schema(description = "공연장 주소", example = "서울특별시 송파구 올림픽로 424")
    val address: String?,

    @Schema(description = "공연장 대표번호", example = "02-1234-5678")
    val phoneNumber: String?,

    @Schema(description = "좌석 배치도 존재 여부", example = "true")
    val hasSeatingChart: Boolean,

    @Schema(description = "공연장 정보 생성일시", example = "2023-01-01T12:00:00")
    val createdAt: LocalDateTime?,

    @Schema(description = "공연장 정보 수정일시", example = "2023-01-01T12:00:00")
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun from(venue: Venue): VenueResponse {
            return VenueResponse(
                id = venue.id,
                name = venue.name,
                address = venue.address,
                phoneNumber = venue.phoneNumber,
                hasSeatingChart = !venue.seatingChart.isNullOrBlank(),
                createdAt = venue.createdAt,
                updatedAt = venue.updatedAt
            )
        }
    }
}

@Schema(description = "공연장 생성 요청 DTO")
data class CreateVenueRequest(
    @field:NotBlank(message = "공연장 이름은 비워둘 수 없습니다.")
    @field:Size(max = 100, message = "공연장 이름은 100자를 초과할 수 없습니다.")
    @Schema(description = "생성할 공연장 이름", example = "올림픽공원 올림픽홀", required = true)
    val name: String,

    @field:Size(max = 255, message = "주소는 255자를 초과할 수 없습니다.")
    @Schema(description = "생성할 공연장 주소", example = "서울특별시 송파구 올림픽로 424")
    val address: String?,

    @field:Size(max = 50, message = "대표번호는 50자를 초과할 수 없습니다.")
    @Schema(description = "공연장 대표번호", example = "02-1234-5678")
    val phoneNumber: String? = null
)

@Schema(description = "공연장 수정 요청 DTO")
data class UpdateVenueRequest(
    @field:NotBlank(message = "공연장 이름은 비워둘 수 없습니다.")
    @field:Size(max = 100, message = "공연장 이름은 100자를 초과할 수 없습니다.")
    @Schema(description = "수정할 공연장 이름", example = "새로운 공연장 이름")
    val name: String,

    @field:Size(max = 255, message = "주소는 255자를 초과할 수 없습니다.")
    @Schema(description = "수정할 공연장 주소", example = "변경된 주소")
    val address: String?,

    @field:Size(max = 50, message = "대표번호는 50자를 초과할 수 없습니다.")
    @Schema(description = "공연장 대표번호", example = "02-1234-5678")
    val phoneNumber: String? = null
)

@Schema(description = "좌석 배치도 저장 요청 DTO")
data class SeatingChartRequest(
    @Schema(description = "좌석 배치도 JSON", required = true)
    val seatingChart: String
)

@Schema(description = "좌석 배치도 응답 DTO")
data class SeatingChartResponse(
    @Schema(description = "공연장 ID", example = "1")
    val venueId: Long,

    @Schema(description = "좌석 배치도 JSON")
    val seatingChart: String?
)
