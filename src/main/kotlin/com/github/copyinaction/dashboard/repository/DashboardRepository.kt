package com.github.copyinaction.dashboard.repository

import com.github.copyinaction.booking.domain.Booking
import com.github.copyinaction.booking.domain.BookingStatus
import com.github.copyinaction.venue.domain.SeatGrade
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Repository
interface DashboardRepository : JpaRepository<Booking, UUID> {

    // ========== 대시보드 요약 쿼리 ==========

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN b.bookingStatus = 'CONFIRMED' THEN b.totalPrice ELSE 0 END), 0) as totalRevenue,
            COUNT(b) as totalBookings,
            SUM(CASE WHEN b.bookingStatus = 'CONFIRMED' THEN 1 ELSE 0 END) as confirmedBookings,
            SUM(CASE WHEN b.bookingStatus = 'CANCELLED' THEN 1 ELSE 0 END) as cancelledBookings,
            SUM(CASE WHEN b.bookingStatus = 'EXPIRED' THEN 1 ELSE 0 END) as expiredBookings
        FROM Booking b
        WHERE b.createdAt >= :startDate AND b.createdAt < :endDate
    """)
    fun getSummaryStats(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): SummaryStatsResult

    @Query("""
        SELECT COUNT(bs)
        FROM BookingSeat bs
        WHERE bs.booking.bookingStatus = 'CONFIRMED'
          AND bs.booking.createdAt >= :startDate
          AND bs.booking.createdAt < :endDate
    """)
    fun getTotalTicketsSold(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): Long

    // ========== 공연별 매출 쿼리 ==========

    @Query("""
        SELECT
            p.id as performanceId,
            p.title as title,
            p.category as category,
            v.name as venueName,
            p.startDate as startDate,
            p.endDate as endDate,
            COALESCE(SUM(CASE WHEN b.bookingStatus = 'CONFIRMED' THEN b.totalPrice ELSE 0 END), 0) as totalRevenue,
            COUNT(b) as totalBookings,
            SUM(CASE WHEN b.bookingStatus = 'CONFIRMED' THEN 1 ELSE 0 END) as confirmedBookings,
            SUM(CASE WHEN b.bookingStatus = 'CANCELLED' THEN 1 ELSE 0 END) as cancelledBookings
        FROM Performance p
        LEFT JOIN p.venue v
        LEFT JOIN PerformanceSchedule ps ON ps.performance = p
        LEFT JOIN Booking b ON b.schedule = ps
            AND b.createdAt >= :startDate AND b.createdAt < :endDate
        GROUP BY p.id, p.title, p.category, v.name, p.startDate, p.endDate
        ORDER BY SUM(CASE WHEN b.bookingStatus = 'CONFIRMED' THEN b.totalPrice ELSE 0 END) DESC
    """)
    fun findPerformanceSales(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        pageable: Pageable
    ): Page<PerformanceSalesResult>

    @Query("""
        SELECT COUNT(bs)
        FROM BookingSeat bs
        JOIN bs.booking b
        JOIN b.schedule ps
        WHERE ps.performance.id = :performanceId
          AND b.bookingStatus = 'CONFIRMED'
          AND b.createdAt >= :startDate
          AND b.createdAt < :endDate
    """)
    fun getTicketsSoldByPerformance(
        @Param("performanceId") performanceId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): Long

    // ========== 일별 매출 쿼리 ==========

    @Query("""
        SELECT
            CAST(b.createdAt AS LocalDate) as date,
            COALESCE(SUM(CASE WHEN b.bookingStatus = 'CONFIRMED' THEN b.totalPrice ELSE 0 END), 0) as revenue,
            COUNT(b) as bookings
        FROM Booking b
        WHERE b.createdAt >= :startDate AND b.createdAt < :endDate
          AND (:performanceId IS NULL OR b.schedule.performance.id = :performanceId)
        GROUP BY CAST(b.createdAt AS LocalDate)
        ORDER BY CAST(b.createdAt AS LocalDate)
    """)
    fun findDailySales(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        @Param("performanceId") performanceId: Long?
    ): List<DailySalesResult>

    @Query("""
        SELECT
            CAST(bs.booking.createdAt AS LocalDate) as date,
            COUNT(bs) as ticketsSold
        FROM BookingSeat bs
        WHERE bs.booking.bookingStatus = 'CONFIRMED'
          AND bs.booking.createdAt >= :startDate
          AND bs.booking.createdAt < :endDate
          AND (:performanceId IS NULL OR bs.booking.schedule.performance.id = :performanceId)
        GROUP BY CAST(bs.booking.createdAt AS LocalDate)
    """)
    fun findDailyTicketsSold(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        @Param("performanceId") performanceId: Long?
    ): List<DailyTicketsSoldResult>

    // ========== 등급별 매출 쿼리 ==========

    @Query("""
        SELECT
            bs.grade as grade,
            SUM(bs.price) as revenue,
            COUNT(bs) as ticketsSold
        FROM BookingSeat bs
        JOIN bs.booking b
        WHERE b.bookingStatus = 'CONFIRMED'
          AND b.schedule.performance.id = :performanceId
          AND b.createdAt >= :startDate
          AND b.createdAt < :endDate
        GROUP BY bs.grade
    """)
    fun findSalesByGrade(
        @Param("performanceId") performanceId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<GradeSalesResult>

    // ========== 회차별 매출 쿼리 ==========

    @Query("""
        SELECT
            ps.id as scheduleId,
            ps.showDateTime as performanceDate,
            COALESCE(SUM(CASE WHEN b.bookingStatus = 'CONFIRMED' THEN b.totalPrice ELSE 0 END), 0) as revenue,
            SUM(CASE WHEN b.bookingStatus = 'CONFIRMED' THEN 1 ELSE 0 END) as confirmedBookings
        FROM PerformanceSchedule ps
        LEFT JOIN Booking b ON b.schedule = ps
        WHERE ps.performance.id = :performanceId
        GROUP BY ps.id, ps.showDateTime
        ORDER BY ps.showDateTime
    """)
    fun findScheduleSalesByPerformance(
        @Param("performanceId") performanceId: Long
    ): List<ScheduleSalesResult>

    @Query("""
        SELECT COUNT(bs)
        FROM BookingSeat bs
        WHERE bs.booking.schedule.id = :scheduleId
          AND bs.booking.bookingStatus = 'CONFIRMED'
    """)
    fun getSoldSeatsBySchedule(@Param("scheduleId") scheduleId: Long): Long

    @Query("""
        SELECT COUNT(bs)
        FROM BookingSeat bs
        WHERE bs.booking.schedule.id = :scheduleId
          AND bs.booking.bookingStatus = 'PENDING'
    """)
    fun getPendingSeatsBySchedule(@Param("scheduleId") scheduleId: Long): Long

    // ========== 회차별 등급별 매출 쿼리 ==========

    @Query("""
        SELECT
            bs.grade as grade,
            SUM(bs.price) as revenue,
            COUNT(bs) as ticketsSold
        FROM BookingSeat bs
        JOIN bs.booking b
        WHERE b.schedule.id = :scheduleId
          AND b.bookingStatus = 'CONFIRMED'
        GROUP BY bs.grade
    """)
    fun findSalesByGradeForSchedule(
        @Param("scheduleId") scheduleId: Long
    ): List<GradeSalesResult>

    @Query("""
        SELECT
            bs.grade as grade,
            COUNT(bs) as count
        FROM BookingSeat bs
        JOIN bs.booking b
        WHERE b.schedule.id = :scheduleId
          AND b.bookingStatus = 'PENDING'
        GROUP BY bs.grade
    """)
    fun findPendingSeatsByGradeForSchedule(
        @Param("scheduleId") scheduleId: Long
    ): List<GradePendingResult>

    // ========== 최근 예매 쿼리 ==========

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.siteUser
        JOIN FETCH b.schedule ps
        JOIN FETCH ps.performance
        WHERE (:bookingStatus IS NULL OR b.bookingStatus = :bookingStatus)
          AND (:performanceId IS NULL OR ps.performance.id = :performanceId)
        ORDER BY b.createdAt DESC
    """)
    fun findRecentBookings(
        @Param("bookingStatus") bookingStatus: BookingStatus?,
        @Param("performanceId") performanceId: Long?,
        pageable: Pageable
    ): List<Booking>

    // ========== 특정 회차의 최근 예매 ==========

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.siteUser
        JOIN FETCH b.schedule ps
        JOIN FETCH ps.performance
        WHERE b.schedule.id = :scheduleId
        ORDER BY b.createdAt DESC
    """)
    fun findRecentBookingsBySchedule(
        @Param("scheduleId") scheduleId: Long,
        pageable: Pageable
    ): List<Booking>
}

// ========== Result Interfaces ==========

interface SummaryStatsResult {
    val totalRevenue: Long
    val totalBookings: Long
    val confirmedBookings: Long
    val cancelledBookings: Long
    val expiredBookings: Long
}

interface PerformanceSalesResult {
    val performanceId: Long
    val title: String
    val category: String
    val venueName: String?
    val startDate: LocalDate
    val endDate: LocalDate
    val totalRevenue: Long
    val totalBookings: Long
    val confirmedBookings: Long
    val cancelledBookings: Long
}

interface DailySalesResult {
    val date: LocalDate
    val revenue: Long
    val bookings: Long
}

interface DailyTicketsSoldResult {
    val date: LocalDate
    val ticketsSold: Long
}

interface GradeSalesResult {
    val grade: SeatGrade
    val revenue: Long
    val ticketsSold: Long
}

interface GradePendingResult {
    val grade: SeatGrade
    val count: Long
}

interface ScheduleSalesResult {
    val scheduleId: Long
    val performanceDate: LocalDateTime
    val revenue: Long
    val confirmedBookings: Long
}
