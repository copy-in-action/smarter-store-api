package com.github.copyinaction.home.dto

import com.github.copyinaction.home.domain.HomeSection
import com.github.copyinaction.home.domain.HomeSectionTag
import com.github.copyinaction.home.domain.PerformanceHomeTag
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

// ========== 사용자용 Response DTOs ==========

@Schema(description = "홈 전체 섹션 응답")
data class HomeSectionsResponse(
    @Schema(description = "섹션 목록")
    val sections: List<HomeSectionResponse>
)

@Schema(description = "홈 섹션 응답")
data class HomeSectionResponse(
    @Schema(description = "섹션 코드")
    val section: HomeSection,

    @Schema(description = "섹션 표시명", example = "인기티켓")
    val displayName: String,

    @Schema(description = "섹션 순서", example = "1")
    val displayOrder: Int,

    @Schema(description = "태그 목록")
    val tags: List<HomeTagWithPerformancesResponse>
)

@Schema(description = "태그별 공연 목록 응답")
data class HomeTagWithPerformancesResponse(
    @Schema(description = "태그 코드")
    val tag: HomeSectionTag,

    @Schema(description = "태그 표시명", example = "뮤지컬")
    val displayName: String,

    @Schema(description = "태그 순서", example = "1")
    val displayOrder: Int,

    @Schema(description = "공연 목록")
    val performances: List<HomePerformanceResponse>
)

@Schema(description = "홈 화면용 공연 간략 정보")
data class HomePerformanceResponse(
    @Schema(description = "공연 ID", example = "1")
    val id: Long,

    @Schema(description = "공연 제목", example = "뮤지컬 위키드")
    val title: String,

    @Schema(description = "메인 이미지 URL")
    val mainImageUrl: String?,

    @Schema(description = "공연 시작일", example = "2025-02-01")
    val startDate: LocalDate,

    @Schema(description = "공연 종료일", example = "2025-03-31")
    val endDate: LocalDate,

    @Schema(description = "공연장명", example = "블루스퀘어")
    val venueName: String?
) {
    companion object {
        fun from(entity: PerformanceHomeTag): HomePerformanceResponse {
            val performance = entity.performance
            return HomePerformanceResponse(
                id = performance.id,
                title = performance.title,
                mainImageUrl = performance.mainImageUrl,
                startDate = performance.startDate,
                endDate = performance.endDate,
                venueName = performance.venue?.name
            )
        }
    }
}
