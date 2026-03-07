package com.github.copyinaction.performance.repository

import com.github.copyinaction.performance.dto.*
import org.springframework.data.domain.Page

interface PerformanceRepositoryCustom {
    /**
     * 필터 및 정렬이 적용된 공연 검색
     */
    fun searchPerformances(
        request: PerformanceSearchRequest
    ): Page<PerformanceSearchResponse>

    /**
     * 검색어 기반 자동완성 (최대 6개)
     */
    fun autocompletePerformances(keyword: String?): List<PerformanceAutocompleteResponse>
}
