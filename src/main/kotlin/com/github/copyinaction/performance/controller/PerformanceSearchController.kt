package com.github.copyinaction.performance.controller

import com.github.copyinaction.auth.service.CustomUserDetails
import com.github.copyinaction.performance.dto.*
import com.github.copyinaction.performance.service.PerformanceSearchService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/performances/search")
@Tag(name = "performance-search", description = "공연 검색 및 자동완성 API")
class PerformanceSearchController(
    private val performanceSearchService: PerformanceSearchService
) {

    @GetMapping
    @Operation(
        summary = "공연 검색 및 필터링",
        description = """
            제목, 카테고리, 공연장 주소를 통합 검색하고 필터 및 정렬을 적용합니다. (권한: 누구나)
            
            **필터 상세**:
            - `status`: 판매 상태 (`PerformanceSearchStatus` - UPCOMING, ON_SALE, CLOSED)
            - `category`: 장르 (String - 뮤지컬, 콘서트, 연극 등)
            - `region`: 지역 (`Region` - SEOUL, GYEONGGI 등 17개 행정구역)
            - `sort`: 정렬 방식 (`PerformanceSearchSort` - BOOKING_COUNT, END_DATE_ASC, CREATED_AT_DESC)
        """
    )
    fun search(
        request: PerformanceSearchRequest
    ): PerformanceSearchListResponse {
        return performanceSearchService.searchPerformances(request)
    }

    @GetMapping("/autocomplete")
    @Operation(summary = "검색 자동완성", description = "검색어 입력 시 최대 6개의 관련 공연을 추천합니다. (권한: 누구나)")
    fun autocomplete(keyword: String?): List<PerformanceAutocompleteResponse> {
        return performanceSearchService.autocompletePerformances(keyword)
    }
}
