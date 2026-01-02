package com.github.copyinaction.dashboard.controller

import com.github.copyinaction.booking.domain.BookingStatus
import com.github.copyinaction.dashboard.dto.*
import com.github.copyinaction.dashboard.service.DashboardService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@Tag(name = "Admin Dashboard", description = "관리자 대시보드 API")
@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
class AdminDashboardController(
    private val dashboardService: DashboardService
) {

    @Operation(
        summary = "대시보드 요약 조회",
        description = """
            전체 매출 및 예매 현황 요약을 조회합니다.
            - 총 매출액, 예매 건수, 티켓 판매 수
            - 이전 기간 대비 변화율

            **권한: ADMIN**
        """
    )
    @GetMapping("/summary")
    fun getSummary(
        @Parameter(description = "조회 시작일 (기본: 오늘)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startDate: LocalDate?,

        @Parameter(description = "조회 종료일 (기본: 오늘)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endDate: LocalDate?
    ): ResponseEntity<DashboardSummaryResponse> {
        val response = dashboardService.getSummary(startDate, endDate)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "공연별 매출 목록 조회",
        description = """
            공연별 매출 현황을 조회합니다.
            - 공연별 총 매출, 예매 건수, 티켓 판매 수
            - 판매율, 평균 티켓 가격

            **권한: ADMIN**
        """
    )
    @GetMapping("/performances")
    fun getPerformanceSales(
        @Parameter(description = "조회 시작일")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startDate: LocalDate?,

        @Parameter(description = "조회 종료일")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endDate: LocalDate?,

        @PageableDefault(size = 20, sort = ["totalRevenue"], direction = Sort.Direction.DESC)
        pageable: Pageable
    ): ResponseEntity<Page<PerformanceSalesResponse>> {
        val response = dashboardService.getPerformanceSales(startDate, endDate, pageable)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "공연 상세 매출 조회",
        description = """
            특정 공연의 상세 매출 현황을 조회합니다.
            - 매출 요약, 등급별 매출, 회차별 매출
            - 일별 매출 추이

            **권한: ADMIN**
        """
    )
    @GetMapping("/performances/{performanceId}")
    fun getPerformanceDetailSales(
        @Parameter(description = "공연 ID")
        @PathVariable performanceId: Long
    ): ResponseEntity<PerformanceDetailSalesResponse> {
        val response = dashboardService.getPerformanceDetailSales(performanceId)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "일별 매출 추이 조회",
        description = """
            기간별 일별 매출 추이를 조회합니다.
            - 일별 매출액, 예매 건수, 티켓 판매 수
            - 특정 공연 필터링 가능

            **권한: ADMIN**
        """
    )
    @GetMapping("/sales/daily")
    fun getDailySales(
        @Parameter(description = "조회 시작일", required = true)
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startDate: LocalDate,

        @Parameter(description = "조회 종료일", required = true)
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endDate: LocalDate,

        @Parameter(description = "공연 ID (선택)")
        @RequestParam(required = false)
        performanceId: Long?
    ): ResponseEntity<DailySalesResponse> {
        val response = dashboardService.getDailySales(startDate, endDate, performanceId)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "회차별 판매 현황 조회",
        description = """
            특정 회차의 상세 판매 현황을 조회합니다.
            - 총 매출, 판매/대기/가능 좌석 수
            - 등급별 판매 현황
            - 최근 예매 내역

            **권한: ADMIN**
        """
    )
    @GetMapping("/schedules/{scheduleId}/sales")
    fun getScheduleSales(
        @Parameter(description = "회차 ID")
        @PathVariable scheduleId: Long
    ): ResponseEntity<ScheduleSalesResponse> {
        val response = dashboardService.getScheduleSales(scheduleId)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/bookings/recent")
    @Operation(summary = "최근 예매 내역 조회")
    fun getRecentBookings(
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(required = false) bookingStatus: BookingStatus?,
        @RequestParam(required = false) performanceId: Long?
    ): ResponseEntity<RecentBookingsResponse> {
        val response = dashboardService.getRecentBookings(limit, bookingStatus, performanceId)
        return ResponseEntity.ok(response)
    }
}
