package com.github.copyinaction.home.dto

import com.github.copyinaction.home.domain.HomeSection
import com.github.copyinaction.home.domain.HomeSectionTag
import com.github.copyinaction.home.domain.PerformanceHomeTag
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

// ========== Request DTOs ==========

@Schema(description = "홈 태그 추가 요청")
data class AddHomeTagRequest(
    @field:NotNull(message = "태그는 필수입니다")
    @Schema(description = "홈 섹션 태그", example = "MUSICAL")
    val tag: HomeSectionTag,

    @field:Min(value = 0, message = "순서는 0 이상이어야 합니다")
    @Schema(description = "노출 순서 (미입력 시 마지막에 추가)", example = "1")
    val displayOrder: Int? = null
)

@Schema(description = "태그 내 공연 순서 변경 요청")
data class UpdateDisplayOrderRequest(
    @field:Valid
    @field:NotNull(message = "순서 목록은 필수입니다")
    @Schema(description = "공연 순서 목록")
    val performanceOrders: List<PerformanceOrderItem>
)

@Schema(description = "공연 순서 항목")
data class PerformanceOrderItem(
    @field:NotNull(message = "공연 ID는 필수입니다")
    @Schema(description = "공연 ID", example = "1")
    val performanceId: Long,

    @field:NotNull(message = "순서는 필수입니다")
    @field:Min(value = 0, message = "순서는 0 이상이어야 합니다")
    @Schema(description = "노출 순서", example = "1")
    val displayOrder: Int
)

// ========== Response DTOs ==========

@Schema(description = "홈 태그 응답")
data class PerformanceHomeTagResponse(
    @Schema(description = "태그 ID", example = "1")
    val id: Long,

    @Schema(description = "홈 섹션 태그")
    val tag: HomeSectionTag,

    @Schema(description = "태그 화면 표시명", example = "뮤지컬")
    val tagDisplayName: String,

    @Schema(description = "섹션")
    val section: HomeSection,

    @Schema(description = "섹션 화면 표시명", example = "인기티켓")
    val sectionDisplayName: String,

    @Schema(description = "노출 순서", example = "1")
    val displayOrder: Int,

    @Schema(description = "자동 태깅 여부", example = "false")
    val isAutoTagged: Boolean,

    @Schema(description = "생성일시")
    val createdAt: LocalDateTime?
) {
    companion object {
        fun from(entity: PerformanceHomeTag): PerformanceHomeTagResponse {
            return PerformanceHomeTagResponse(
                id = entity.id,
                tag = entity.tag,
                tagDisplayName = entity.tag.displayName,
                section = entity.tag.section,
                sectionDisplayName = entity.tag.section.displayName,
                displayOrder = entity.displayOrder,
                isAutoTagged = entity.isAutoTagged,
                createdAt = entity.createdAt
            )
        }
    }
}

@Schema(description = "태그 내 공연 응답 (관리자용)")
data class TagPerformanceResponse(
    @Schema(description = "태그 매핑 ID", example = "1")
    val tagId: Long,

    @Schema(description = "공연 ID", example = "1")
    val performanceId: Long,

    @Schema(description = "공연 제목", example = "뮤지컬 위키드")
    val title: String,

    @Schema(description = "공연 메인 이미지 URL")
    val mainImageUrl: String?,

    @Schema(description = "노출 순서", example = "1")
    val displayOrder: Int,

    @Schema(description = "자동 태깅 여부", example = "false")
    val isAutoTagged: Boolean,

    @Schema(description = "공연 노출 여부", example = "true")
    val visible: Boolean
) {
    companion object {
        fun from(entity: PerformanceHomeTag): TagPerformanceResponse {
            return TagPerformanceResponse(
                tagId = entity.id,
                performanceId = entity.performance.id,
                title = entity.performance.title,
                mainImageUrl = entity.performance.mainImageUrl,
                displayOrder = entity.displayOrder,
                isAutoTagged = entity.isAutoTagged,
                visible = entity.performance.visible
            )
        }
    }
}
