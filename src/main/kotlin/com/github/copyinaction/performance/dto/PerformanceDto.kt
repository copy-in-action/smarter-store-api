package com.github.copyinaction.performance.dto

import com.github.copyinaction.performance.domain.Performance
import com.github.copyinaction.venue.dto.VenueResponse
import io.swagger.v3.oas.annotations.media.Schema
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

    @Schema(description = "공연 상세 설명", example = "이 공연은...")
    val description: String?,

    @Schema(description = "공연 카테고리", example = "콘서트")
    val category: String,

    @Schema(description = "공연 시간 (분)", example = "120")
    val runningTime: Int?,

    @Schema(description = "공연 관람 연령", example = "15세 이상 관람가")
    val ageRating: String?,

    @Schema(description = "공연 대표 이미지 URL", example = "https://example.com/main.jpg")
    val mainImageUrl: String?,

    @Schema(description = "공연 노출 여부", example = "true")
    val visible: Boolean,

    @Schema(description = "공연이 열리는 공연장 정보")
    val venue: VenueResponse?,

    @Schema(description = "공연 시작일", example = "2025-12-20")
    val startDate: LocalDate,

    @Schema(description = "공연 종료일", example = "2025-12-31")
    val endDate: LocalDate,

    @Schema(description = "출연진", example = "효린, 다솜")
    val actors: String?,

    @Schema(description = "기획사", example = "미쓰잭슨 주식회사")
    val agency: String?,

    @Schema(description = "제작사", example = "CJ ENM")
    val producer: String?,

    @Schema(description = "주최", example = "인터파크")
    val host: String?,

    @Schema(description = "할인정보", example = "조기예매 10% 할인")
    val discountInfo: String?,

    @Schema(description = "이용안내", example = "본 공연은 1인 4매까지 예매 가능합니다.")
    val usageGuide: String?,

    @Schema(description = "취소/환불규정", example = "공연 7일 전까지 전액 환불 가능")
    val refundPolicy: String?,

    @Schema(description = "상품상세 이미지 URL", example = "https://example.com/detail.jpg")
    val detailImageUrl: String?,

    @Schema(description = "판매자/기획사 정보")
    val company: CompanyResponse?,

    @Schema(description = "예매 수수료", example = "1000")
    val bookingFee: Int?,

    @Schema(description = "배송 안내", example = "현장 수령만 가능합니다.")
    val shippingGuide: String?,

    @Schema(description = "공연 정보 생성일시", example = "2023-01-01T12:00:00")
    val createdAt: LocalDateTime?,

    @Schema(description = "공연 정보 수정일시", example = "2023-01-01T12:00:00")
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
                visible = performance.visible,
                venue = performance.venue?.let { VenueResponse.from(it) },
                startDate = performance.startDate,
                endDate = performance.endDate,
                actors = performance.actors,
                agency = performance.agency,
                producer = performance.producer,
                host = performance.host,
                discountInfo = performance.discountInfo,
                usageGuide = performance.usageGuide,
                refundPolicy = performance.refundPolicy,
                detailImageUrl = performance.detailImageUrl,
                company = performance.company?.let { CompanyResponse.from(it) },
                bookingFee = performance.bookingFee,
                shippingGuide = performance.shippingGuide,
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
    @Schema(description = "생성할 공연명", example = "2025 HYOLYN CONCERT", required = true)
    val title: String,

    @Schema(description = "생성할 공연 상세 설명", example = "이 공연은...")
    val description: String?,

    @field:NotBlank
    @Schema(description = "생성할 공연 카테고리", example = "콘서트", required = true)
    val category: String,

    @Schema(description = "생성할 공연 시간 (분)", example = "120")
    val runningTime: Int?,

    @Schema(description = "생성할 공연 관람 연령", example = "15세 이상 관람가")
    val ageRating: String?,

    @Schema(description = "생성할 공연 대표 이미지 URL", example = "https://example.com/main.jpg")
    val mainImageUrl: String?,

    @Schema(description = "공연 노출 여부", example = "false")
    val visible: Boolean = false,

    @Schema(description = "공연이 열릴 공연장 ID", example = "1")
    val venueId: Long?,

    @field:NotNull
    @Schema(description = "생성할 공연 시작일", example = "2025-12-20", required = true)
    val startDate: LocalDate,

    @field:NotNull
    @Schema(description = "생성할 공연 종료일", example = "2025-12-31", required = true)
    val endDate: LocalDate,

    @Schema(description = "출연진", example = "효린, 다솜")
    val actors: String? = null,

    @field:Size(max = 255)
    @Schema(description = "기획사", example = "미쓰잭슨 주식회사")
    val agency: String? = null,

    @field:Size(max = 255)
    @Schema(description = "제작사", example = "CJ ENM")
    val producer: String? = null,

    @field:Size(max = 255)
    @Schema(description = "주최", example = "인터파크")
    val host: String? = null,

    @Schema(description = "할인정보", example = "조기예매 10% 할인")
    val discountInfo: String? = null,

    @Schema(description = "이용안내", example = "본 공연은 1인 4매까지 예매 가능합니다.")
    val usageGuide: String? = null,

    @Schema(description = "취소/환불규정", example = "공연 7일 전까지 전액 환불 가능")
    val refundPolicy: String? = null,

    @field:Size(max = 500)
    @Schema(description = "상품상세 이미지 URL", example = "https://example.com/detail.jpg")
    val detailImageUrl: String? = null,

    @Schema(description = "판매자/기획사 ID", example = "1")
    val companyId: Long? = null,

    @Schema(description = "예매 수수료", example = "1000")
    val bookingFee: Int? = null,

    @Schema(description = "배송 안내", example = "현장 수령만 가능합니다.")
    val shippingGuide: String? = null
)

@Schema(description = "공연 수정 요청 DTO")
data class UpdatePerformanceRequest(
    @field:NotBlank
    @field:Size(max = 255)
    @Schema(description = "수정할 공연명", example = "2025 HYOLYN CONCERT", required = true)
    val title: String,

    @Schema(description = "수정할 공연 상세 설명", example = "이 공연은...")
    val description: String?,

    @field:NotBlank
    @Schema(description = "수정할 공연 카테고리", example = "콘서트", required = true)
    val category: String,

    @Schema(description = "수정할 공연 시간 (분)", example = "120")
    val runningTime: Int?,

    @Schema(description = "수정할 공연 관람 연령", example = "15세 이상 관람가")
    val ageRating: String?,

    @Schema(description = "수정할 공연 대표 이미지 URL", example = "https://example.com/main.jpg")
    val mainImageUrl: String?,

    @Schema(description = "공연 노출 여부", example = "true")
    val visible: Boolean,

    @Schema(description = "공연이 열릴 공연장 ID", example = "1")
    val venueId: Long?,

    @field:NotNull
    @Schema(description = "수정할 공연 시작일", example = "2025-12-20", required = true)
    val startDate: LocalDate,

    @field:NotNull
    @Schema(description = "수정할 공연 종료일", example = "2025-12-31", required = true)
    val endDate: LocalDate,

    @Schema(description = "출연진", example = "효린, 다솜")
    val actors: String? = null,

    @field:Size(max = 255)
    @Schema(description = "기획사", example = "미쓰잭슨 주식회사")
    val agency: String? = null,

    @field:Size(max = 255)
    @Schema(description = "제작사", example = "CJ ENM")
    val producer: String? = null,

    @field:Size(max = 255)
    @Schema(description = "주최", example = "인터파크")
    val host: String? = null,

    @Schema(description = "할인정보", example = "조기예매 10% 할인")
    val discountInfo: String? = null,

    @Schema(description = "이용안내", example = "본 공연은 1인 4매까지 예매 가능합니다.")
    val usageGuide: String? = null,

    @Schema(description = "취소/환불규정", example = "공연 7일 전까지 전액 환불 가능")
    val refundPolicy: String? = null,

    @field:Size(max = 500)
    @Schema(description = "상품상세 이미지 URL", example = "https://example.com/detail.jpg")
    val detailImageUrl: String? = null,

    @Schema(description = "판매자/기획사 ID", example = "1")
    val companyId: Long? = null,

    @Schema(description = "예매 수수료", example = "1000")
    val bookingFee: Int? = null,

    @Schema(description = "배송 안내", example = "현장 수령만 가능합니다.")
    val shippingGuide: String? = null
)
