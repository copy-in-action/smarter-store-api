package com.github.copyinaction.stats.domain

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*
import org.hibernate.annotations.Comment
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "performance_sales_stats")
@Comment("공연별 매출 통계")
@Schema(description = "공연별 매출 통계 응답")
class PerformanceSalesStats(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("통계 ID")
    @Schema(description = "통계 ID", example = "1")
    val id: Long = 0,

    @Column(nullable = false)
    @Comment("공연 ID")
    @Schema(description = "공연 ID", example = "1")
    val performanceId: Long,

    @Column(nullable = false)
    @Comment("날짜")
    @Schema(description = "날짜", example = "2026-01-05")
    val date: LocalDate,

    @Column(nullable = false)
    @Comment("총 매출액")
    @Schema(description = "총 매출액", example = "5000000")
    var totalRevenue: Long = 0,

    @Column(nullable = false)
    @Comment("판매 티켓 수")
    @Schema(description = "판매 티켓 수", example = "100")
    var ticketsSold: Int = 0,

    @Column(nullable = false)
    @Comment("전체 좌석 수")
    @Schema(description = "전체 좌석 수", example = "120")
    var totalSeats: Int = 0,

    @Column(nullable = false)
    @Comment("좌석 점유율 (%)")
    @Schema(description = "좌석 점유율 (%)", example = "83.33")
    var occupancyRate: Double = 0.0,

    @Column(nullable = false)
    @Comment("티켓 평균 판매 가격")
    @Schema(description = "티켓 평균 판매 가격", example = "50000")
    var avgTicketPrice: Int = 0,

    @Column(nullable = false)
    @Comment("총 할인 금액")
    @Schema(description = "총 할인 금액", example = "200000")
    var totalDiscountAmount: Long = 0,

    @Column(nullable = false)
    @Comment("수정일시")
    @Schema(description = "수정일시")
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
