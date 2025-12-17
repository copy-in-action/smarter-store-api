package com.github.copyinaction.notice.dto

import com.github.copyinaction.notice.domain.NoticeCategory
import com.github.copyinaction.notice.domain.TicketingNotice
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(description = "예매 안내사항 생성 요청")
data class CreateTicketingNoticeRequest(
    @field:NotNull(message = "카테고리는 필수입니다")
    @Schema(description = "카테고리", example = "BOOKING_NOTICE")
    val category: NoticeCategory,

    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(max = 255, message = "제목은 255자 이내여야 합니다")
    @Schema(description = "제목", example = "예매 전 확인사항")
    val title: String,

    @field:NotBlank(message = "내용은 필수입니다")
    @Schema(description = "내용", example = "예매 전 반드시 공연 일시와 좌석을 확인해주세요.")
    val content: String,

    @Schema(description = "정렬 순서", example = "1", defaultValue = "0")
    val displayOrder: Int = 0,

    @Schema(description = "활성화 여부", example = "true", defaultValue = "true")
    val isActive: Boolean = true
)

@Schema(description = "예매 안내사항 수정 요청")
data class UpdateTicketingNoticeRequest(
    @field:NotNull(message = "카테고리는 필수입니다")
    @Schema(description = "카테고리", example = "BOOKING_NOTICE")
    val category: NoticeCategory,

    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(max = 255, message = "제목은 255자 이내여야 합니다")
    @Schema(description = "제목", example = "예매 전 확인사항")
    val title: String,

    @field:NotBlank(message = "내용은 필수입니다")
    @Schema(description = "내용", example = "예매 전 반드시 공연 일시와 좌석을 확인해주세요.")
    val content: String,

    @Schema(description = "정렬 순서", example = "1")
    val displayOrder: Int,

    @Schema(description = "활성화 여부", example = "true")
    val isActive: Boolean
)

@Schema(description = "예매 안내사항 응답")
data class TicketingNoticeResponse(
    @Schema(description = "안내사항 ID", example = "1")
    val id: Long,

    @Schema(description = "카테고리", example = "BOOKING_NOTICE")
    val category: NoticeCategory,

    @Schema(description = "카테고리 설명", example = "예매 유의사항")
    val categoryDescription: String,

    @Schema(description = "제목", example = "예매 전 확인사항")
    val title: String,

    @Schema(description = "내용", example = "예매 전 반드시 공연 일시와 좌석을 확인해주세요.")
    val content: String,

    @Schema(description = "정렬 순서", example = "1")
    val displayOrder: Int,

    @Schema(description = "활성화 여부", example = "true")
    val isActive: Boolean,

    @Schema(description = "생성일시")
    val createdAt: LocalDateTime?,

    @Schema(description = "수정일시")
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun from(notice: TicketingNotice): TicketingNoticeResponse {
            return TicketingNoticeResponse(
                id = notice.id,
                category = notice.category,
                categoryDescription = notice.category.description,
                title = notice.title,
                content = notice.content,
                displayOrder = notice.displayOrder,
                isActive = notice.isActive,
                createdAt = notice.createdAt,
                updatedAt = notice.updatedAt
            )
        }
    }
}

@Schema(description = "카테고리별 안내사항 그룹 응답")
data class TicketingNoticeGroupResponse(
    @Schema(description = "카테고리", example = "BOOKING_NOTICE")
    val category: NoticeCategory,

    @Schema(description = "카테고리 설명", example = "예매 유의사항")
    val categoryDescription: String,

    @Schema(description = "안내사항 목록")
    val notices: List<TicketingNoticeResponse>
)
