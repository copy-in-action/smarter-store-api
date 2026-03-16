package com.github.copyinaction.performance.dto

import com.github.copyinaction.performance.domain.Region
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

/**
 * 공연 검색 요청 파라미터
 */
data class PerformanceSearchRequest(
    @Schema(description = "검색어 (제목, 카테고리, 공연장 주소 통합 검색)", example = "콘서트")
    val keyword: String? = null,

    @Schema(description = "판매 상태 필터 (다중 선택 가능)", example = "[\"ON_SALE\", \"UPCOMING\"]")
    val status: List<PerformanceSearchStatus>? = null,

    @Schema(description = "장르(카테고리) 필터 (다중 선택 가능)", example = "[\"뮤지컬\", \"콘서트\"]")
    val category: List<String>? = null,

    @Schema(description = "지역 필터 (다중 선택 가능)", example = "[\"SEOUL\", \"GYEONGGI\"]")
    val region: List<Region>? = null,

    @Schema(description = "정렬 방식", example = "CREATED_AT_DESC")
    val sort: PerformanceSearchSort? = PerformanceSearchSort.CREATED_AT_DESC,

    @Schema(description = "페이지 번호 (0-based)", example = "0")
    val page: Int = 0,

    @Schema(description = "페이지 크기", example = "10")
    val size: Int = 10
)

/**
 * 검색 결과 리스트 응답 DTO
 */
data class PerformanceSearchListResponse(
    @Schema(description = "검색 결과 목록")
    val content: List<PerformanceSearchResponse>,

    @Schema(description = "전체 데이터 개수")
    val totalElements: Long,

    @Schema(description = "다음 페이지 존재 여부")
    val hasNextPage: Boolean
)

/**
 * 검색 결과 개별 항목 DTO
 */
data class PerformanceSearchResponse(
    @Schema(description = "공연 ID")
    val id: Long,

    @Schema(description = "공연 제목")
    val title: String,

    @Schema(description = "공연 대표 이미지 URL")
    val mainImageUrl: String?,

    @Schema(description = "카테고리")
    val category: String,

    @Schema(description = "지역명")
    val regionName: String?,

    @Schema(description = "공연장 주소")
    val venueAddress: String?,

    @Schema(description = "공연 시작일")
    val startDate: LocalDate,

    @Schema(description = "공연 종료일")
    val endDate: LocalDate
) {
    companion object {
        fun from(response: PerformanceSearchResponse, regionName: String?): PerformanceSearchResponse {
            return response.copy(regionName = regionName)
        }
    }
}

/**
 * 검색창 자동완성 응답 DTO
 */
data class PerformanceAutocompleteResponse(
    @Schema(description = "공연 ID")
    val id: Long,

    @Schema(description = "공연 제목")
    val title: String,

    @Schema(description = "대표 이미지 URL")
    val mainImageUrl: String?,

    @Schema(description = "카테고리")
    val category: String,

    @Schema(description = "지역명 (17개 행정구역)")
    val regionName: String?
)

/**
 * 공연 검색 정렬 방식
 */
@Schema(enumAsRef = true, description = "공연 검색 정렬 방식")
enum class PerformanceSearchSort(val label: String) {
    @Schema(description = "예매 많은 순")
    BOOKING_COUNT("예매 많은 순"),
    @Schema(description = "종료 임박 순")
    END_DATE_ASC("종료 임박 순"),
    @Schema(description = "최근 등록 순")
    CREATED_AT_DESC("최근 등록 순")
}

/**
 * 공연 검색 상태 필터
 */
@Schema(enumAsRef = true, description = "공연 검색 상태 필터")
enum class PerformanceSearchStatus(val label: String) {
    @Schema(description = "판매 예정")
    UPCOMING("판매 예정"),
    @Schema(description = "판매 중")
    ON_SALE("판매 중"),
    @Schema(description = "판매 종료")
    CLOSED("판매 종료")
}
