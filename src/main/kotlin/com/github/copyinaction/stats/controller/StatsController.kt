package com.github.copyinaction.stats.controller

import com.github.copyinaction.stats.domain.DailySalesStats
import com.github.copyinaction.stats.domain.DiscountStats
import com.github.copyinaction.stats.domain.PaymentMethodStats
import com.github.copyinaction.stats.domain.PerformanceSalesStats
import com.github.copyinaction.stats.service.SalesStatsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@Tag(name = "admin-stats", description = "관리자용 통계 API")
@RestController
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
class StatsController(
    private val salesStatsService: SalesStatsService
) {

    @Operation(summary = "일별 매출 통계 조회")
    @GetMapping("/daily")
    fun getDailySales(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<DailySalesStats> {
        val stats = salesStatsService.getDailySales(date)
        return ResponseEntity.ok(stats)
    }

    @Operation(summary = "공연별 매출 통계 조회")
    @GetMapping("/performance/{id}")
    fun getPerformanceSales(
        @PathVariable id: Long
    ): ResponseEntity<List<PerformanceSalesStats>> {
        val stats = salesStatsService.getPerformanceSales(id)
        return ResponseEntity.ok(stats)
    }

    @Operation(summary = "결제 수단별 통계 조회")
    @GetMapping("/payment-methods")
    fun getPaymentMethodStats(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<List<PaymentMethodStats>> {
        val stats = salesStatsService.getPaymentMethodStats(date)
        return ResponseEntity.ok(stats)
    }

    @Operation(summary = "할인 종류별 통계 조회")
    @GetMapping("/discounts")
    fun getDiscountStats(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<List<DiscountStats>> {
        val stats = salesStatsService.getDiscountStats(date)
        return ResponseEntity.ok(stats)
    }
}
