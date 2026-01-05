package com.github.copyinaction.stats.repository

import com.github.copyinaction.payment.domain.PaymentMethod
import com.github.copyinaction.stats.domain.PaymentMethodStats
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface PaymentMethodStatsRepository : JpaRepository<PaymentMethodStats, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PaymentMethodStats s WHERE s.paymentMethod = :method AND s.date = :date")
    fun findByPaymentMethodAndDateWithLock(method: PaymentMethod, date: LocalDate): Optional<PaymentMethodStats>
    
    fun findAllByDate(date: LocalDate): List<PaymentMethodStats>
}
