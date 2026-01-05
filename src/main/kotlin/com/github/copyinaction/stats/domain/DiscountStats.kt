package com.github.copyinaction.stats.domain

import com.github.copyinaction.discount.domain.DiscountType
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "discount_stats")
class DiscountStats(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val discountType: DiscountType,

    @Column(nullable = false)
    val date: LocalDate,

    @Column(nullable = false)
    var usageCount: Int = 0,

    @Column(nullable = false)
    var totalDiscountAmount: Long = 0,

    @Column(nullable = false)
    var avgDiscountAmount: Int = 0,

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun increment(amount: Long) {
        this.usageCount++
        this.totalDiscountAmount += amount
        if (usageCount > 0) {
            this.avgDiscountAmount = (totalDiscountAmount / usageCount).toInt()
        }
        this.updatedAt = LocalDateTime.now()
    }
}
