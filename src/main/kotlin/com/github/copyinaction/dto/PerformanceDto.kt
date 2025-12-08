package com.github.copyinaction.dto

import com.github.copyinaction.domain.Performance
import com.github.copyinaction.domain.Venue
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "공연 정보 응답 DTO")
data class PerformanceResponse(
    @Schema(description = "공연 ID", example = "1")
    val id: Long,

    @Schema(description = "공연명", example = "2025 HYOLYN CONCERT")
    val title: String,

    @Schema(description = "공연 설명")
    val description: String?,

    @Schema(description = "카테고리", example = "콘서트")
    val category: String,

    @Schema(description = "공연 시간 (분)", example = "120")
    val runningTime: Int?,

    @Schema(description = "관람 연령", example = "15세 이상 관람가")
    val ageRating: String?,

    @Schema(description = "대표 이미지 URL", example = "https://example.com/main.jpg")
    val mainImageUrl: String?,

    @Schema(description = "공연장 정보")
    val venue: VenueResponse?,

    @Schema(description = "공연 시작일", example = "2025-12-20")
    val startDate: LocalDate,

    @Schema(description = "공연 종료일", example = "2025-12-31")
    val endDate: LocalDate,

    @Schema(description = "생성일시")
    val createdAt: LocalDateTime?,

    @Schema(description = "수정일시")
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun from(performance: Performance): PerformanceResponse {
            return PerformanceResponse(
                id = performance.id,
                title = performance.title,
                description = performance.description,
                category = performance.category,
                runningTime = performance.runningTime,
                ageRating = performance.ageRating,
                mainImageUrl = performance.mainImageUrl,
                venue = performance.venue?.let { VenueResponse.from(it) },
                startDate = performance.startDate,
                endDate = performance.endDate,
                createdAt = performance.createdAt,
                updatedAt = performance.updatedAt
            )
        }
    }
}

@Schema(description = "공연 생성 요청 DTO")
data class CreatePerformanceRequest(
    @field:NotBlank
    @field:Size(max = 255)
    @Schema(description = "공연명", example = "2025 HYOLYN CONCERT", required = true)
    val title: String,

    @Schema(description = "공연 설명")
    val description: String?,

    @field:NotBlank
    @Schema(description = "카테고리", example = "콘서트", required = true)
    val category: String,

    @Schema(description = "공연 시간 (분)", example = "120")
    val runningTime: Int?,

    @Schema(description = "관람 연령", example = "15세 이상 관람가")
    val ageRating: String?,

    @Schema(description = "대표 이미지 URL", example = "https://example.com/main.jpg")
    val mainImageUrl: String?,

    @Schema(description = "공연장 ID", example = "1")
    val venueId: Long?,

    @field:NotNull
    @field:FutureOrPresent
    @Schema(description = "공연 시작일", example = "2025-12-20", required = true)
    val startDate: LocalDate,

    @field:NotNull
    @field:FutureOrPresent
    @Schema(description = "공연 종료일", example = "2025-12-31", required = true)
    val endDate: LocalDate
) {
    fun toEntity(venue: Venue?): Performance {
        return Performance(
            title = this.title,
            description = this.description,
            category = this.category,
            runningTime = this.runningTime,
            ageRating = this.ageRating,
            mainImageUrl = this.mainImageUrl,
            venue = venue,
            startDate = this.startDate,
            endDate = this.endDate
        )
    }
}

@Schema(description = "공연 수정 요청 DTO")
data class UpdatePerformanceRequest(
    @field:NotBlank
    @field:Size(max = 255)
    @Schema(description = "공연명", example = "2025 HYOLYN CONCERT", required = true)
    val title: String,

    @Schema(description = "공연 설명")
    val description: String?,

    @field:NotBlank
    @Schema(description = "카테고리", example = "콘서트", required = true)
    val category: String,

    @Schema(description = "공연 시간 (분)", example = "120")
    val runningTime: Int?,

    @Schema(description = "관람 연령", example = "15세 이상 관람가")
    val ageRating: String?,

    @Schema(description = "대표 이미지 URL", example = "https://example.com/main.jpg")
    val mainImageUrl: String?,

    @Schema(description = "공연장 ID", example = "1")
    val venueId: Long?,

    @field:NotNull
    @field:FutureOrPresent
    @Schema(description = "공연 시작일", example = "2025-12-20", required = true)
    val startDate: LocalDate,

    @field:NotNull
    @field:FutureOrPresent
    @Schema(description = "공연 종료일", example = "2025-12-31", required = true)
    val endDate: LocalDate
)
