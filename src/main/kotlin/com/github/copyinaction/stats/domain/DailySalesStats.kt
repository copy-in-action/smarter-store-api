package com.github.copyinaction.stats.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "daily_sales_stats")
class DailySalesStats(
    @Id
    @Column(nullable = false)
    val date: LocalDate,

    @Column(nullable = false)
    var totalSales: Long = 0,

    @Column(nullable = false)
    var totalTransactions: Int = 0,

    @Column(nullable = false)
    var totalTickets: Int = 0,

    @Column(nullable = false)
    var totalDiscounts: Long = 0,

    @Column(nullable = false)
    var totalRefunds: Long = 0,

    @Column(nullable = false)
    var netSales: Long = 0,

    @Column(nullable = false)
    var avgOrderValue: Int = 0,

    @Column(nullable = false)
    var avgTicketPrice: Int = 0,

    @Column(nullable = false)
    var cancelledTransactions: Int = 0,

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun incrementSales(amount: Long, ticketCount: Int, discountAmount: Long) {
        this.totalSales += amount
        this.totalTransactions++
        this.totalTickets += ticketCount
        this.totalDiscounts += discountAmount
        this.netSales = totalSales - totalRefunds
        recalculateAverages()
        this.updatedAt = LocalDateTime.now()
    }

    fun recordRefund(amount: Long) {
        this.totalRefunds += amount
        this.netSales = totalSales - totalRefunds
        this.updatedAt = LocalDateTime.now()
    }

    fun recordCancel() {
        this.cancelledTransactions++
        this.updatedAt = LocalDateTime.now()
    }

    private fun recalculateAverages() {
        if (totalTransactions > 0) {
            this.avgOrderValue = (totalSales / totalTransactions).toInt()
        }
        if (totalTickets > 0) {
            this.avgTicketPrice = (totalSales / totalTickets).toInt()
        }
    }
}
