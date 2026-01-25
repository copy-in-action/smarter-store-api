package com.github.copyinaction.performance.repository

import com.github.copyinaction.performance.domain.Performance
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PerformanceRepository : JpaRepository<Performance, Long> {

    @EntityGraph(attributePaths = ["venue", "company"])
    override fun findById(id: Long): Optional<Performance>

    @EntityGraph(attributePaths = ["venue", "company"])
    override fun findAll(): List<Performance>

    @EntityGraph(attributePaths = ["venue", "company"])
    fun findAllByVisible(visible: Boolean): List<Performance>

    fun existsByVenueId(venueId: Long): Boolean
}
