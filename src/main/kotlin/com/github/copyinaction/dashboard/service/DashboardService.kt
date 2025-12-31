package com.github.copyinaction.dashboard.service

import com.github.copyinaction.booking.domain.Booking
import com.github.copyinaction.booking.domain.BookingStatus
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.dashboard.dto.*
import com.github.copyinaction.dashboard.repository.DashboardRepository
import com.github.copyinaction.performance.repository.PerformanceRepository
import com.github.copyinaction.performance.repository.PerformanceScheduleRepository
import com.github.copyinaction.performance.repository.TicketOptionRepository
import com.github.copyinaction.venue.domain.SeatGrade
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class DashboardService(
    private val dashboardRepository: DashboardRepository,
    private val performanceRepository: PerformanceRepository,
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val ticketOptionRepository: TicketOptionRepository
) {

    /**
     * 대시보드 요약 조회
     */
    fun getSummary(startDate: LocalDate?, endDate: LocalDate?): DashboardSummaryResponse {
        val effectiveStartDate = startDate ?: LocalDate.now()
        val effectiveEndDate = endDate ?: LocalDate.now()

        val startDateTime = effectiveStartDate.atStartOfDay()
        val endDateTime = effectiveEndDate.plusDays(1).atStartOfDay()

        // 현재 기간 통계
        val stats = dashboardRepository.getSummaryStats(startDateTime, endDateTime)
        val ticketsSold = dashboardRepository.getTotalTicketsSold(startDateTime, endDateTime)

        // 이전 기간 통계 (동일 기간)
        val periodDays = java.time.temporal.ChronoUnit.DAYS.between(effectiveStartDate, effectiveEndDate) + 1
        val prevStartDate = effectiveStartDate.minusDays(periodDays)
        val prevEndDate = effectiveEndDate.minusDays(periodDays)
        val prevStartDateTime = prevStartDate.atStartOfDay()
        val prevEndDateTime = prevEndDate.plusDays(1).atStartOfDay()

        val prevStats = dashboardRepository.getSummaryStats(prevStartDateTime, prevEndDateTime)

        val comparison = if (prevStats.totalRevenue > 0 || prevStats.totalBookings > 0) {
            ComparisonData(
                revenueChangePercent = calculateChangePercent(stats.totalRevenue, prevStats.totalRevenue),
                bookingsChangePercent = calculateChangePercent(stats.totalBookings, prevStats.totalBookings)
            )
        } else null

        val averagePrice = if (ticketsSold > 0) stats.totalRevenue / ticketsSold else 0L

        return DashboardSummaryResponse(
            period = DatePeriod(effectiveStartDate, effectiveEndDate),
            totalRevenue = stats.totalRevenue,
            totalBookings = stats.totalBookings,
            confirmedBookings = stats.confirmedBookings,
            cancelledBookings = stats.cancelledBookings,
            expiredBookings = stats.expiredBookings,
            averageTicketPrice = averagePrice,
            totalTicketsSold = ticketsSold,
            comparisonWithPrevious = comparison
        )
    }

    /**
     * 공연별 매출 목록 조회
     */
    fun getPerformanceSales(
        startDate: LocalDate?,
        endDate: LocalDate?,
        pageable: Pageable
    ): Page<PerformanceSalesResponse> {
        val effectiveStartDate = startDate ?: LocalDate.now().minusMonths(1)
        val effectiveEndDate = endDate ?: LocalDate.now()

        val startDateTime = effectiveStartDate.atStartOfDay()
        val endDateTime = effectiveEndDate.plusDays(1).atStartOfDay()

        val salesPage = dashboardRepository.findPerformanceSales(startDateTime, endDateTime, pageable)

        val responses = salesPage.content.map { result ->
            val ticketsSold = dashboardRepository.getTicketsSoldByPerformance(
                result.performanceId, startDateTime, endDateTime
            )
            val averagePrice = if (ticketsSold > 0) result.totalRevenue / ticketsSold else 0L

            // 총 좌석 수 계산 (해당 공연의 모든 회차)
            val totalSeats = getTotalSeatsByPerformance(result.performanceId)
            val salesRate = if (totalSeats > 0) (ticketsSold.toDouble() / totalSeats) * 100 else 0.0

            PerformanceSalesResponse(
                performanceId = result.performanceId,
                title = result.title,
                category = result.category,
                venue = result.venueName,
                period = DatePeriod(result.startDate, result.endDate),
                totalRevenue = result.totalRevenue,
                totalBookings = result.totalBookings,
                confirmedBookings = result.confirmedBookings,
                cancelledBookings = result.cancelledBookings,
                totalTicketsSold = ticketsSold,
                averageTicketPrice = averagePrice,
                salesRate = String.format("%.1f", salesRate).toDouble()
            )
        }

        return PageImpl(responses, pageable, salesPage.totalElements)
    }

    /**
     * 공연 상세 매출 조회
     */
    fun getPerformanceDetailSales(performanceId: Long): PerformanceDetailSalesResponse {
        val performance = performanceRepository.findById(performanceId)
            .orElseThrow { CustomException(ErrorCode.PERFORMANCE_NOT_FOUND) }

        val startDateTime = LocalDateTime.MIN
        val endDateTime = LocalDateTime.now().plusYears(10)

        // 티켓 판매 통계
        val ticketsSold = dashboardRepository.getTicketsSoldByPerformance(performanceId, startDateTime, endDateTime)
        val totalSeats = getTotalSeatsByPerformance(performanceId)

        // 등급별 매출
        val gradeSales = dashboardRepository.findSalesByGrade(performanceId, startDateTime, endDateTime)
        val revenueByGrade = gradeSales.map { result ->
            val avgPrice = if (result.ticketsSold > 0) result.revenue / result.ticketsSold else 0L
            GradeSalesResponse(
                grade = result.grade,
                revenue = result.revenue,
                ticketsSold = result.ticketsSold,
                averagePrice = avgPrice
            )
        }

        // 회차별 매출
        val scheduleSales = dashboardRepository.findScheduleSalesByPerformance(performanceId)
        val schedules = scheduleSales.map { result ->
            val soldSeats = dashboardRepository.getSoldSeatsBySchedule(result.scheduleId)
            val scheduleTotalSeats = getScheduleTotalSeats(result.scheduleId)
            val salesRate = if (scheduleTotalSeats > 0) (soldSeats.toDouble() / scheduleTotalSeats) * 100 else 0.0

            ScheduleSalesSummary(
                scheduleId = result.scheduleId,
                performanceDate = result.performanceDate,
                totalSeats = scheduleTotalSeats,
                soldSeats = soldSeats,
                revenue = result.revenue,
                salesRate = String.format("%.1f", salesRate).toDouble()
            )
        }

        // 일별 매출 (최근 30일)
        val dailyStartDate = LocalDate.now().minusDays(30)
        val dailySales = getDailySalesData(dailyStartDate, LocalDate.now(), performanceId)

        val salesRate = if (totalSeats > 0) (ticketsSold.toDouble() / totalSeats) * 100 else 0.0

        // 해당 공연의 예매 통계 조회
        val performanceStats = getPerformanceBookingStats(performanceId)

        return PerformanceDetailSalesResponse(
            performance = PerformanceInfo(
                id = performance.id,
                title = performance.title,
                category = performance.category,
                venue = performance.venue?.name,
                period = DatePeriod(performance.startDate, performance.endDate)
            ),
            summary = SalesSummary(
                totalRevenue = performanceStats.totalRevenue,
                totalBookings = performanceStats.totalBookings,
                confirmedBookings = performanceStats.confirmedBookings,
                cancelledBookings = performanceStats.cancelledBookings,
                expiredBookings = performanceStats.expiredBookings,
                totalTicketsSold = ticketsSold,
                totalSeats = totalSeats,
                salesRate = String.format("%.1f", salesRate).toDouble()
            ),
            revenueByGrade = revenueByGrade,
            schedules = schedules,
            dailySales = dailySales
        )
    }

    /**
     * 일별 매출 추이 조회
     */
    fun getDailySales(
        startDate: LocalDate,
        endDate: LocalDate,
        performanceId: Long?
    ): DailySalesResponse {
        val dailyData = getDailySalesData(startDate, endDate, performanceId)

        val totalRevenue = dailyData.sumOf { it.revenue }
        val totalBookings = dailyData.sumOf { it.bookings }

        return DailySalesResponse(
            period = DatePeriod(startDate, endDate),
            totalRevenue = totalRevenue,
            totalBookings = totalBookings,
            dailyData = dailyData
        )
    }

    /**
     * 회차별 판매 현황 조회
     */
    fun getScheduleSales(scheduleId: Long): ScheduleSalesResponse {
        val schedule = performanceScheduleRepository.findById(scheduleId)
            .orElseThrow { CustomException(ErrorCode.PERFORMANCE_SCHEDULE_NOT_FOUND) }

        val performance = schedule.performance

        // 판매 현황
        val soldSeats = dashboardRepository.getSoldSeatsBySchedule(scheduleId)
        val pendingSeats = dashboardRepository.getPendingSeatsBySchedule(scheduleId)
        val totalSeats = getScheduleTotalSeats(scheduleId)
        val availableSeats = totalSeats - soldSeats - pendingSeats

        // 매출 계산
        val gradeSales = dashboardRepository.findSalesByGradeForSchedule(scheduleId)
        val totalRevenue = gradeSales.sumOf { it.revenue }
        val totalBookings = getScheduleBookingCount(scheduleId)
        val confirmedBookings = getScheduleBookingCountByStatus(scheduleId, BookingStatus.CONFIRMED)
        val cancelledBookings = getScheduleBookingCountByStatus(scheduleId, BookingStatus.CANCELLED)

        val salesRate = if (totalSeats > 0) (soldSeats.toDouble() / totalSeats) * 100 else 0.0

        // 등급별 판매 현황
        val pendingByGrade = dashboardRepository.findPendingSeatsByGradeForSchedule(scheduleId)
            .associate { it.grade to it.count }

        val ticketOptions = ticketOptionRepository.findByPerformanceScheduleId(scheduleId)
        val salesByGrade = ticketOptions.map { option ->
            val gradeStats = gradeSales.find { it.grade == option.seatGrade }
            val gradePending = pendingByGrade[option.seatGrade] ?: 0L
            val gradeSold = gradeStats?.ticketsSold ?: 0L
            val gradeRevenue = gradeStats?.revenue ?: 0L
            val gradeSalesRate = if (option.totalQuantity > 0)
                (gradeSold.toDouble() / option.totalQuantity) * 100 else 0.0

            GradeSalesDetail(
                grade = option.seatGrade,
                totalSeats = option.totalQuantity.toLong(),
                soldSeats = gradeSold,
                pendingSeats = gradePending,
                revenue = gradeRevenue,
                salesRate = String.format("%.1f", gradeSalesRate).toDouble()
            )
        }

        // 최근 예매
        val recentBookings = dashboardRepository.findRecentBookingsBySchedule(
            scheduleId, PageRequest.of(0, 10)
        ).map { it.toRecentBookingResponse() }

        return ScheduleSalesResponse(
            schedule = ScheduleInfo(
                id = schedule.id,
                performanceId = performance.id,
                performanceTitle = performance.title,
                performanceDate = schedule.showDateTime,
                venue = performance.venue?.name
            ),
            sales = ScheduleSalesDetail(
                totalRevenue = totalRevenue,
                totalBookings = totalBookings,
                confirmedBookings = confirmedBookings,
                cancelledBookings = cancelledBookings,
                totalSeats = totalSeats,
                soldSeats = soldSeats,
                pendingSeats = pendingSeats,
                availableSeats = availableSeats.coerceAtLeast(0),
                salesRate = String.format("%.1f", salesRate).toDouble()
            ),
            salesByGrade = salesByGrade,
            recentBookings = recentBookings
        )
    }

    /**
     * 최근 예매 내역 조회
     */
    fun getRecentBookings(
        limit: Int,
        status: BookingStatus?,
        performanceId: Long?
    ): RecentBookingsResponse {
        val effectiveLimit = limit.coerceIn(1, 50)
        val bookings = dashboardRepository.findRecentBookings(
            status, performanceId, PageRequest.of(0, effectiveLimit)
        )

        return RecentBookingsResponse(
            bookings = bookings.map { it.toRecentBookingResponse() }
        )
    }

    // ========== Private Helper Methods ==========

    private fun calculateChangePercent(current: Long, previous: Long): Double {
        if (previous == 0L) return if (current > 0) 100.0 else 0.0
        return String.format("%.1f", ((current - previous).toDouble() / previous) * 100).toDouble()
    }

    private fun getTotalSeatsByPerformance(performanceId: Long): Long {
        val schedules = performanceScheduleRepository.findByPerformanceId(performanceId)
        return schedules.sumOf { schedule ->
            ticketOptionRepository.findByPerformanceScheduleId(schedule.id)
                .sumOf { it.totalQuantity.toLong() }
        }
    }

    private fun getScheduleTotalSeats(scheduleId: Long): Long {
        return ticketOptionRepository.findByPerformanceScheduleId(scheduleId)
            .sumOf { it.totalQuantity.toLong() }
    }

    private fun getDailySalesData(
        startDate: LocalDate,
        endDate: LocalDate,
        performanceId: Long?
    ): List<DailySalesData> {
        val startDateTime = startDate.atStartOfDay()
        val endDateTime = endDate.plusDays(1).atStartOfDay()

        val dailySales = dashboardRepository.findDailySales(startDateTime, endDateTime, performanceId)
        val dailyTickets = dashboardRepository.findDailyTicketsSold(startDateTime, endDateTime, performanceId)
            .associate { it.date to it.ticketsSold }

        // 모든 날짜 채우기
        val allDates = generateSequence(startDate) { it.plusDays(1) }
            .takeWhile { !it.isAfter(endDate) }
            .toList()

        val salesMap = dailySales.associate { it.date to it }

        return allDates.map { date ->
            val sales = salesMap[date]
            DailySalesData(
                date = date,
                revenue = sales?.revenue ?: 0L,
                bookings = sales?.bookings ?: 0L,
                ticketsSold = dailyTickets[date] ?: 0L
            )
        }
    }

    private fun getPerformanceBookingStats(performanceId: Long): PerformanceBookingStats {
        val schedules = performanceScheduleRepository.findByPerformanceId(performanceId)
        val scheduleIds = schedules.map { it.id }

        if (scheduleIds.isEmpty()) {
            return PerformanceBookingStats(0, 0, 0, 0, 0)
        }

        var totalRevenue = 0L
        var totalBookings = 0L
        var confirmedBookings = 0L
        var cancelledBookings = 0L
        var expiredBookings = 0L

        scheduleIds.forEach { scheduleId ->
            val gradeSales = dashboardRepository.findSalesByGradeForSchedule(scheduleId)
            totalRevenue += gradeSales.sumOf { it.revenue }
            totalBookings += getScheduleBookingCount(scheduleId)
            confirmedBookings += getScheduleBookingCountByStatus(scheduleId, BookingStatus.CONFIRMED)
            cancelledBookings += getScheduleBookingCountByStatus(scheduleId, BookingStatus.CANCELLED)
            expiredBookings += getScheduleBookingCountByStatus(scheduleId, BookingStatus.EXPIRED)
        }

        return PerformanceBookingStats(totalRevenue, totalBookings, confirmedBookings, cancelledBookings, expiredBookings)
    }

    private fun getScheduleBookingCount(scheduleId: Long): Long {
        return dashboardRepository.findRecentBookingsBySchedule(scheduleId, PageRequest.of(0, Int.MAX_VALUE)).size.toLong()
    }

    private fun getScheduleBookingCountByStatus(scheduleId: Long, status: BookingStatus): Long {
        return dashboardRepository.findRecentBookingsBySchedule(scheduleId, PageRequest.of(0, Int.MAX_VALUE))
            .count { it.status == status }.toLong()
    }

    private fun Booking.toRecentBookingResponse(): RecentBookingResponse {
        return RecentBookingResponse(
            bookingId = this.id.toString(),
            bookingNumber = this.bookingNumber,
            status = this.status.name,
            performanceTitle = this.schedule.performance.title,
            scheduleDate = this.schedule.showDateTime,
            userName = maskName(this.user.username),
            userEmail = maskEmail(this.user.email),
            ticketCount = this.bookingSeats.size,
            totalPrice = this.totalPrice,
            createdAt = this.createdAt!!
        )
    }

    private fun maskName(name: String): String {
        return when {
            name.length <= 1 -> name
            name.length == 2 -> "${name[0]}*"
            else -> "${name[0]}${"*".repeat(name.length - 2)}${name.last()}"
        }
    }

    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        val localPart = parts[0]
        val domain = parts[1]
        val maskedLocal = when {
            localPart.length <= 2 -> "${localPart[0]}***"
            else -> "${localPart.take(2)}***"
        }
        return "$maskedLocal@$domain"
    }

    private data class PerformanceBookingStats(
        val totalRevenue: Long,
        val totalBookings: Long,
        val confirmedBookings: Long,
        val cancelledBookings: Long,
        val expiredBookings: Long
    )
}
