package com.github.copyinaction.stats.repository

import com.github.copyinaction.stats.domain.PerformanceSalesStats
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface PerformanceSalesStatsRepository : JpaRepository<PerformanceSalesStats, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PerformanceSalesStats s WHERE s.performanceId = :performanceId AND s.date = :date")
    fun findByPerformanceIdAndDateWithLock(performanceId: Long, date: LocalDate): Optional<PerformanceSalesStats>
    
    fun findAllByPerformanceIdOrderByDateDesc(performanceId: Long): List<PerformanceSalesStats>

    fun findAllByDate(date: LocalDate): List<PerformanceSalesStats>
}
