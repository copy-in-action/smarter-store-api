package com.github.copyinaction.stats.domain

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Comment
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "daily_sales_stats")
@Comment("일별 매출 통계")
@Schema(description = "일별 매출 통계 응답")
class DailySalesStats(
    @Id
    @Column(nullable = false)
    @Comment("날짜")
    @Schema(description = "날짜", example = "2026-01-05")
    val date: LocalDate,

    @Column(nullable = false)
    @Comment("총 매출액")
    @Schema(description = "총 매출액", example = "1500000")
    var totalSales: Long = 0,

    @Column(nullable = false)
    @Comment("총 거래 건수")
    @Schema(description = "총 거래 건수", example = "150")
    var totalTransactions: Int = 0,

    @Column(nullable = false)
    @Comment("총 판매 티켓 수")
    @Schema(description = "총 판매 티켓 수", example = "300")
    var totalTickets: Int = 0,

    @Column(nullable = false)
    @Comment("총 할인 금액")
    @Schema(description = "총 할인 금액", example = "50000")
    var totalDiscounts: Long = 0,

    @Column(nullable = false)
    @Comment("총 환불 금액")
    @Schema(description = "총 환불 금액", example = "20000")
    var totalRefunds: Long = 0,

    @Column(nullable = false)
    @Comment("순 매출액 (총 매출 - 총 환불)")
    @Schema(description = "순 매출액 (총 매출액 - 총 환불 금액)", example = "1480000")
    var netSales: Long = 0,

    @Column(nullable = false)
    @Comment("건당 평균 결제 금액")
    @Schema(description = "건당 평균 결제 금액", example = "10000")
    var avgOrderValue: Int = 0,

    @Column(nullable = false)
    @Comment("티켓당 평균 판매 금액")
    @Schema(description = "티켓당 평균 판매 금액", example = "5000")
    var avgTicketPrice: Int = 0,

    @Column(nullable = false)
    @Comment("취소 거래 건수")
    @Schema(description = "취소 거래 건수", example = "5")
    var cancelledTransactions: Int = 0,

    @Column(nullable = false)
    @Comment("수정일시")
    @Schema(description = "수정일시")
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
