package com.github.copyinaction.stats.repository

import com.github.copyinaction.discount.domain.DiscountType
import com.github.copyinaction.stats.domain.DiscountStats
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface DiscountStatsRepository : JpaRepository<DiscountStats, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DiscountStats s WHERE s.discountType = :type AND s.date = :date")
    fun findByDiscountTypeAndDateWithLock(type: DiscountType, date: LocalDate): Optional<DiscountStats>
    
    fun findAllByDate(date: LocalDate): List<DiscountStats>
}
