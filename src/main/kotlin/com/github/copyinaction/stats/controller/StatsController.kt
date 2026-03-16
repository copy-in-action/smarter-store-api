package com.github.copyinaction.stats.controller

import com.github.copyinaction.payment.domain.PaymentStatus
import com.github.copyinaction.payment.repository.PaymentRepository
import com.github.copyinaction.stats.domain.DailySalesStats
import com.github.copyinaction.stats.domain.DiscountStats
import com.github.copyinaction.stats.domain.PaymentMethodStats
import com.github.copyinaction.stats.domain.PerformanceSalesStats
import com.github.copyinaction.stats.service.SalesStatsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalTime

@Tag(name = "admin-stats", description = "관리자용 통계 API")
@RestController
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
class StatsController(
    private val salesStatsService: SalesStatsService,
    private val paymentRepository: PaymentRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(summary = "일별 매출 통계 조회", description = "지정된 날짜의 매출 통계를 조회합니다.\n\n**권한: ADMIN**")
    @GetMapping("/daily")
    fun getDailySales(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<DailySalesStats> {
        val stats = salesStatsService.getDailySales(date)
        return ResponseEntity.ok(stats)
    }

    @Operation(summary = "공연별 매출 통계 조회", description = "특정 공연의 전체 매출 통계를 조회합니다.\n\n**권한: ADMIN**")
    @GetMapping("/performance/{id}")
    fun getPerformanceSales(
        @PathVariable id: Long
    ): ResponseEntity<List<PerformanceSalesStats>> {
        val stats = salesStatsService.getPerformanceSales(id)
        return ResponseEntity.ok(stats)
    }

    @Operation(summary = "결제 수단별 통계 조회", description = "지정된 날짜의 결제 수단별 통계를 조회합니다.\n\n**권한: ADMIN**")
    @GetMapping("/payment-methods")
    fun getPaymentMethodStats(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<List<PaymentMethodStats>> {
        val stats = salesStatsService.getPaymentMethodStats(date)
        return ResponseEntity.ok(stats)
    }

    @Operation(summary = "할인 종류별 통계 조회", description = "지정된 날짜의 할인 종류별 통계를 조회합니다.\n\n**권한: ADMIN**")
    @GetMapping("/discounts")
    fun getDiscountStats(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<List<DiscountStats>> {
        val stats = salesStatsService.getDiscountStats(date)
        return ResponseEntity.ok(stats)
    }

    @Operation(summary = "일별 통계 강제 재집계", description = "지정된 날짜의 결제 데이터를 기반으로 통계를 다시 계산합니다.\n\n**권한: ADMIN**")
    @PostMapping("/daily/recalculate")
    fun recalculateDailyStats(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<String> {
        val startDateTime = date.atStartOfDay()
        val endDateTime = date.atTime(LocalTime.MAX)

        val completedPayments = paymentRepository.findAllByStatusAndCompletedAtBetween(
            PaymentStatus.COMPLETED,
            startDateTime,
            endDateTime
        )

        salesStatsService.recalculateDailyStats(date, completedPayments)
        log.info("[Admin API] 일별 통계 재집계 완료: date={}, count={}", date, completedPayments.size)
        return ResponseEntity.ok("통계 재집계 완료: $date (${completedPayments.size}건)")
    }

    @Operation(summary = "주간 통계 강제 재집계", description = "최근 7일간의 결제 데이터를 기반으로 통계를 다시 계산합니다.\n\n**권한: ADMIN**")
    @PostMapping("/weekly/recalculate")
    fun recalculateWeeklyStats(): ResponseEntity<String> {
        val today = LocalDate.now()
        var totalRecalculated = 0

        for (i in 7 downTo 1) {
            val targetDate = today.minusDays(i.toLong())
            val startDateTime = targetDate.atStartOfDay()
            val endDateTime = targetDate.atTime(LocalTime.MAX)

            val completedPayments = paymentRepository.findAllByStatusAndCompletedAtBetween(
                PaymentStatus.COMPLETED,
                startDateTime,
                endDateTime
            )

            if (completedPayments.isNotEmpty()) {
                salesStatsService.recalculateDailyStats(targetDate, completedPayments)
                totalRecalculated += completedPayments.size
            }
        }

        log.info("[Admin API] 주간 통계 재집계 완료: 총 {}건 처리", totalRecalculated)
        return ResponseEntity.ok("주간 통계 재집계 완료 (최근 7일): 총 ${totalRecalculated}건")
    }
}
