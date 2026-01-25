package com.github.copyinaction.home.dto

import com.github.copyinaction.home.domain.HomeSection
import com.github.copyinaction.home.domain.HomeSectionTag
import com.github.copyinaction.home.domain.PerformanceHomeTag
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

// ========== 사용자용 Response DTOs ==========

@Schema(description = "홈 화면 전체 섹션 응답 DTO")
data class HomeSectionsResponse(
    @Schema(description = "홈 화면을 구성하는 대분류 섹션 목록 (순서대로 노출)")
    val sections: List<HomeSectionResponse>
)

@Schema(description = "홈 섹션 정보 응답")
data class HomeSectionResponse(
    @Schema(description = "섹션 코드 (예: POPULAR_TICKET, DATE_COURSE)", example = "POPULAR_TICKET")
    val section: HomeSection,

    @Schema(description = "섹션 화면 표시명 (한글)", example = "인기티켓")
    val displayName: String,

    @Schema(description = "섹션 노출 순서", example = "1")
    val displayOrder: Int,

    @Schema(description = "섹션에 포함된 하위 태그 및 공연 목록")
    val tags: List<HomeTagWithPerformancesResponse>
)

@Schema(description = "태그별 공연 목록 응답")
data class HomeTagWithPerformancesResponse(
    @Schema(description = "태그 코드 (예: MUSICAL, WEEKLY_OPEN)", example = "MUSICAL")
    val tag: HomeSectionTag,

    @Schema(description = "태그 화면 표시명 (한글)", example = "뮤지컬")
    val displayName: String,

    @Schema(description = "태그 노출 순서 (탭 순서 등으로 활용)", example = "1")
    val displayOrder: Int,

    @Schema(description = "해당 태그에 속한 공연 목록 (관리자가 지정한 순서대로 정렬됨)")
    val performances: List<HomePerformanceResponse>
)

@Schema(description = "홈 화면용 공연 간략 정보 DTO")
data class HomePerformanceResponse(
    @Schema(description = "공연 ID (상세 페이지 이동 시 사용)", example = "100")
    val id: Long,

    @Schema(description = "공연 제목", example = "뮤지컬 위키드")
    val title: String,

    @Schema(description = "메인 포스터 이미지 URL")
    val mainImageUrl: String?,

    @Schema(description = "공연 시작일", example = "2025-02-01")
    val startDate: LocalDate,

    @Schema(description = "공연 종료일", example = "2025-03-31")
    val endDate: LocalDate,

    @Schema(description = "공연장 이름", example = "블루스퀘어")
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
