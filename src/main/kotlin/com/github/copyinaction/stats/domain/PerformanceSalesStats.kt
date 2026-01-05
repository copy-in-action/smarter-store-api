package com.github.copyinaction.stats.domain

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "performance_sales_stats")
class PerformanceSalesStats(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val performanceId: Long,

    @Column(nullable = false)
    val date: LocalDate,

    @Column(nullable = false)
    var totalRevenue: Long = 0,

    @Column(nullable = false)
    var ticketsSold: Int = 0,

    @Column(nullable = false)
    var totalSeats: Int = 0,

    @Column(nullable = false)
    var occupancyRate: Double = 0.0,

    @Column(nullable = false)
    var avgTicketPrice: Int = 0,

    @Column(nullable = false)
    var totalDiscountAmount: Long = 0,

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun update(amount: Long, ticketCount: Int, discountAmount: Long, capacity: Int) {
        this.totalRevenue += amount
        this.ticketsSold += ticketCount
        this.totalDiscountAmount += discountAmount
        this.totalSeats = capacity

        if (totalSeats > 0) {
            this.occupancyRate = (ticketsSold.toDouble() / totalSeats.toDouble()) * 100.0
        }
        if (ticketsSold > 0) {
            this.avgTicketPrice = (totalRevenue / ticketsSold).toInt()
        }
        this.updatedAt = LocalDateTime.now()
    }
}
