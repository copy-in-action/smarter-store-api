package com.github.copyinaction.performance.service

import com.github.copyinaction.performance.domain.Region
import com.github.copyinaction.performance.dto.*
import com.github.copyinaction.performance.repository.PerformanceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PerformanceSearchService(
    private val performanceRepository: PerformanceRepository
) {

    /**
     * 검색어 기반 공연 검색 및 필터링
     */
    fun searchPerformances(
        request: PerformanceSearchRequest
    ): PerformanceSearchListResponse {
        // 1. 데이터 조회 (페이징, 필터, 정렬)
        val searchResult = performanceRepository.searchPerformances(request)

        // 2. 지역명 매핑 및 응답 변환
        val content = searchResult.content.map { item ->
            val region = Region.findFromAddress(item.venueAddress)
            PerformanceSearchResponse.from(item, region?.displayName)
        }

        return PerformanceSearchListResponse(
            content = content,
            totalElements = searchResult.totalElements,
            hasNextPage = searchResult.hasNext()
        )
    }

    /**
     * 검색창 자동완성
     */
    fun autocompletePerformances(keyword: String?): List<PerformanceAutocompleteResponse> {
        if (keyword.isNullOrBlank()) return emptyList()

        val results = performanceRepository.autocompletePerformances(keyword)

        return results.map { item ->
            val region = Region.findFromAddress(item.regionName)
            PerformanceAutocompleteResponse(
                id = item.id,
                title = item.title,
                mainImageUrl = item.mainImageUrl,
                category = item.category,
                regionName = region?.displayName
            )
        }
    }
}
