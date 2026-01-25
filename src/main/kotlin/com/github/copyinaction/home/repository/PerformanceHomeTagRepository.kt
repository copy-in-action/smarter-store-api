package com.github.copyinaction.home.repository

import com.github.copyinaction.home.domain.HomeSectionTag
import com.github.copyinaction.home.domain.PerformanceHomeTag
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface PerformanceHomeTagRepository : JpaRepository<PerformanceHomeTag, Long> {

    /**
     * 태그별 공연 목록 조회 (노출 순서대로)
     */
    @EntityGraph(attributePaths = ["performance", "performance.venue"])
    fun findByTagOrderByDisplayOrderAsc(tag: HomeSectionTag): List<PerformanceHomeTag>

    /**
     * 태그별 visible=true인 공연 목록 조회 (사용자용)
     */
    @EntityGraph(attributePaths = ["performance", "performance.venue"])
    @Query("""
        SELECT pht FROM PerformanceHomeTag pht
        WHERE pht.tag = :tag
        AND pht.performance.visible = true
        ORDER BY pht.displayOrder ASC
    """)
    fun findByTagAndVisibleOrderByDisplayOrderAsc(tag: HomeSectionTag): List<PerformanceHomeTag>

    /**
     * 공연별 태그 목록 조회
     */
    fun findByPerformanceId(performanceId: Long): List<PerformanceHomeTag>

    /**
     * 공연-태그 존재 여부 확인
     */
    fun existsByPerformanceIdAndTag(performanceId: Long, tag: HomeSectionTag): Boolean

    /**
     * 공연-태그 삭제
     */
    fun deleteByPerformanceIdAndTag(performanceId: Long, tag: HomeSectionTag)

    /**
     * 공연의 자동 태그 삭제 (지역 기반)
     */
    @Modifying
    @Query("DELETE FROM PerformanceHomeTag pht WHERE pht.performance.id = :performanceId AND pht.isAutoTagged = true")
    fun deleteAutoTagsByPerformanceId(performanceId: Long)

    /**
     * 태그 내 최대 displayOrder 조회
     */
    @Query("SELECT COALESCE(MAX(pht.displayOrder), 0) FROM PerformanceHomeTag pht WHERE pht.tag = :tag")
    fun findMaxDisplayOrderByTag(tag: HomeSectionTag): Int
}
