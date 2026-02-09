package com.github.copyinaction.notice.dto

import com.github.copyinaction.notice.domain.NoticeCategory
import com.github.copyinaction.notice.domain.Notice
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(description = "공지사항 생성 요청")
data class CreateNoticeRequest(
    @field:NotNull(message = "카테고리는 필수입니다")
    @Schema(description = "카테고리", example = "BOOKING_NOTICE")
    val category: NoticeCategory,

    @field:NotBlank(message = "내용은 필수입니다")
    @Schema(description = "내용", example = "예매 전 반드시 공연 일시와 좌석을 확인해주세요.")
    val content: String
)

@Schema(description = "공지사항 수정 요청")
data class UpdateNoticeRequest(
    @field:NotNull(message = "카테고리는 필수입니다")
    @Schema(description = "카테고리", example = "BOOKING_NOTICE")
    val category: NoticeCategory,

    @field:NotBlank(message = "내용은 필수입니다")
    @Schema(description = "내용", example = "예매 전 반드시 공연 일시와 좌석을 확인해주세요.")
    val content: String
)

@Schema(description = "공지사항 상태 수정 요청")
data class NoticeStatusRequest(
    @field:NotNull(message = "활성화 여부는 필수입니다")
    @Schema(description = "활성화 여부", example = "true")
    val isActive: Boolean
)

@Schema(description = "공지사항 응답")
data class NoticeResponse(
    @Schema(description = "공지사항 ID", example = "1")
    val id: Long,

    @Schema(description = "카테고리", example = "BOOKING_NOTICE")
    val category: NoticeCategory,

    @Schema(description = "카테고리 설명", example = "예매 유의사항")
    val categoryDescription: String,

    @Schema(description = "내용", example = "예매 전 반드시 공연 일시와 좌석을 확인해주세요.")
    val content: String,

    @Schema(description = "활성화 여부", example = "true")
    val isActive: Boolean,

    @Schema(description = "생성일시")
    val createdAt: LocalDateTime?,

    @Schema(description = "수정일시")
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun from(notice: Notice): NoticeResponse {
            return NoticeResponse(
                id = notice.id,
                category = notice.category,
                categoryDescription = notice.category.description,
                content = notice.content,
                isActive = notice.isActive,
                createdAt = notice.createdAt,
                updatedAt = notice.updatedAt
            )
        }
    }
}

@Schema(description = "카테고리별 공지사항 그룹 응답")
data class NoticeGroupResponse(
    @Schema(description = "카테고리", example = "BOOKING_NOTICE")
    val category: NoticeCategory,

    @Schema(description = "카테고리 설명", example = "예매 유의사항")
    val categoryDescription: String,

    @Schema(description = "공지사항 목록")
    val notices: List<NoticeResponse>
)
