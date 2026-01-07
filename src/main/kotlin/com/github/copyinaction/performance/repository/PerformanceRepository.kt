package com.github.copyinaction.performance.repository

import com.github.copyinaction.performance.domain.Performance
import com.github.copyinaction.performance.dto.PerformanceSitemapResponse
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface PerformanceRepository : JpaRepository<Performance, Long> {

    @EntityGraph(attributePaths = ["venue", "company"])
    override fun findById(id: Long): Optional<Performance>

    @EntityGraph(attributePaths = ["venue", "company"])
    override fun findAll(): List<Performance>

    fun existsByVenueId(venueId: Long): Boolean

    @Query("SELECT new com.github.copyinaction.performance.dto.PerformanceSitemapResponse(p.id, p.updatedAt) FROM Performance p WHERE p.visible = true")
    fun findAllSitemapData(): List<PerformanceSitemapResponse>
}
