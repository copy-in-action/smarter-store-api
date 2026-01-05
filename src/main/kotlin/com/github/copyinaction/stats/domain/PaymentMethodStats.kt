package com.github.copyinaction.stats.domain

import com.github.copyinaction.payment.domain.PaymentMethod
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "payment_method_stats")
class PaymentMethodStats(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val paymentMethod: PaymentMethod,

    @Column(nullable = false)
    val date: LocalDate,

    @Column(nullable = false)
    var transactionCount: Int = 0,

    @Column(nullable = false)
    var totalAmount: Long = 0,

    @Column(nullable = false)
    var avgAmount: Int = 0,

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun increment(amount: Long) {
        this.transactionCount++
        this.totalAmount += amount
        if (transactionCount > 0) {
            this.avgAmount = (totalAmount / transactionCount).toInt()
        }
        this.updatedAt = LocalDateTime.now()
    }
}
