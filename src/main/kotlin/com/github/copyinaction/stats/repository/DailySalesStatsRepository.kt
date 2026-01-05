package com.github.copyinaction.stats.repository

import com.github.copyinaction.stats.domain.DailySalesStats
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface DailySalesStatsRepository : JpaRepository<DailySalesStats, LocalDate> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DailySalesStats s WHERE s.date = :date")
    fun findByDateWithLock(date: LocalDate): Optional<DailySalesStats>
}
