package com.github.copyinaction.dashboard.dto

import com.github.copyinaction.venue.domain.SeatGrade
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

// ========== 공통 ==========

@Schema(description = "기간 정보")
data class DatePeriod(
    @Schema(description = "시작일", example = "2025-12-01")
    val startDate: LocalDate,
    @Schema(description = "종료일", example = "2025-12-31")
    val endDate: LocalDate
)

@Schema(description = "이전 기간 대비 비교 데이터")
data class ComparisonData(
    @Schema(description = "매출 변화율 (%)", example = "12.5")
    val revenueChangePercent: Double,
    @Schema(description = "예매 건수 변화율 (%)", example = "8.3")
    val bookingsChangePercent: Double
)

// ========== 대시보드 요약 ==========

@Schema(description = "대시보드 요약 응답")
data class DashboardSummaryResponse(
    @Schema(description = "조회 기간")
    val period: DatePeriod,
    @Schema(description = "총 매출액", example = "15000000")
    val totalRevenue: Long,
    @Schema(description = "총 예매 건수", example = "320")
    val totalBookings: Long,
    @Schema(description = "확정 예매 건수", example = "280")
    val confirmedBookings: Long,
    @Schema(description = "취소 예매 건수", example = "25")
    val cancelledBookings: Long,
    @Schema(description = "만료 예매 건수", example = "15")
    val expiredBookings: Long,
    @Schema(description = "평균 티켓 가격", example = "46875")
    val averageTicketPrice: Long,
    @Schema(description = "판매 티켓 수", example = "520")
    val totalTicketsSold: Long,
    @Schema(description = "이전 기간 대비 비교")
    val comparisonWithPrevious: ComparisonData?
)

// ========== 공연별 매출 ==========

@Schema(description = "공연별 매출 응답")
data class PerformanceSalesResponse(
    @Schema(description = "공연 ID", example = "1")
    val performanceId: Long,
    @Schema(description = "공연 제목", example = "뮤지컬 위키드")
    val title: String,
    @Schema(description = "카테고리", example = "뮤지컬")
    val category: String,
    @Schema(description = "공연장명", example = "블루스퀘어 신한카드홀")
    val venue: String?,
    @Schema(description = "공연 기간")
    val period: DatePeriod,
    @Schema(description = "총 매출액", example = "8500000")
    val totalRevenue: Long,
    @Schema(description = "총 예매 건수", example = "180")
    val totalBookings: Long,
    @Schema(description = "확정 예매 건수", example = "165")
    val confirmedBookings: Long,
    @Schema(description = "취소 예매 건수", example = "10")
    val cancelledBookings: Long,
    @Schema(description = "판매 티켓 수", example = "320")
    val totalTicketsSold: Long,
    @Schema(description = "평균 티켓 가격", example = "26562")
    val averageTicketPrice: Long,
    @Schema(description = "판매율 (%)", example = "75.5")
    val salesRate: Double
)

// ========== 공연 상세 매출 ==========

@Schema(description = "공연 기본 정보")
data class PerformanceInfo(
    @Schema(description = "공연 ID", example = "1")
    val id: Long,
    @Schema(description = "공연 제목", example = "뮤지컬 위키드")
    val title: String,
    @Schema(description = "카테고리", example = "뮤지컬")
    val category: String,
    @Schema(description = "공연장명", example = "블루스퀘어 신한카드홀")
    val venue: String?,
    @Schema(description = "공연 기간")
    val period: DatePeriod
)

@Schema(description = "매출 요약")
data class SalesSummary(
    @Schema(description = "총 매출액", example = "8500000")
    val totalRevenue: Long,
    @Schema(description = "총 예매 건수", example = "180")
    val totalBookings: Long,
    @Schema(description = "확정 예매 건수", example = "165")
    val confirmedBookings: Long,
    @Schema(description = "취소 예매 건수", example = "10")
    val cancelledBookings: Long,
    @Schema(description = "만료 예매 건수", example = "5")
    val expiredBookings: Long,
    @Schema(description = "판매 티켓 수", example = "320")
    val totalTicketsSold: Long,
    @Schema(description = "총 좌석 수", example = "500")
    val totalSeats: Long,
    @Schema(description = "판매율 (%)", example = "64.0")
    val salesRate: Double
)

@Schema(description = "등급별 매출")
data class GradeSalesResponse(
    @Schema(description = "좌석 등급")
    val grade: SeatGrade,
    @Schema(description = "매출액", example = "4500000")
    val revenue: Long,
    @Schema(description = "판매 티켓 수", example = "45")
    val ticketsSold: Long,
    @Schema(description = "평균 가격", example = "100000")
    val averagePrice: Long
)

@Schema(description = "회차 매출 요약")
data class ScheduleSalesSummary(
    @Schema(description = "회차 ID", example = "101")
    val scheduleId: Long,
    @Schema(description = "공연 일시")
    val performanceDate: LocalDateTime,
    @Schema(description = "총 좌석 수", example = "100")
    val totalSeats: Long,
    @Schema(description = "판매 좌석 수", example = "85")
    val soldSeats: Long,
    @Schema(description = "매출액", example = "2500000")
    val revenue: Long,
    @Schema(description = "판매율 (%)", example = "85.0")
    val salesRate: Double
)

@Schema(description = "공연 상세 매출 응답")
data class PerformanceDetailSalesResponse(
    @Schema(description = "공연 정보")
    val performance: PerformanceInfo,
    @Schema(description = "매출 요약")
    val summary: SalesSummary,
    @Schema(description = "등급별 매출")
    val revenueByGrade: List<GradeSalesResponse>,
    @Schema(description = "회차별 매출")
    val schedules: List<ScheduleSalesSummary>,
    @Schema(description = "일별 매출")
    val dailySales: List<DailySalesData>
)

// ========== 일별 매출 ==========

@Schema(description = "일별 매출 데이터")
data class DailySalesData(
    @Schema(description = "날짜", example = "2025-12-01")
    val date: LocalDate,
    @Schema(description = "매출액", example = "450000")
    val revenue: Long,
    @Schema(description = "예매 건수", example = "12")
    val bookings: Long,
    @Schema(description = "판매 티켓 수", example = "18")
    val ticketsSold: Long
)

@Schema(description = "일별 매출 추이 응답")
data class DailySalesResponse(
    @Schema(description = "조회 기간")
    val period: DatePeriod,
    @Schema(description = "총 매출액", example = "15000000")
    val totalRevenue: Long,
    @Schema(description = "총 예매 건수", example = "320")
    val totalBookings: Long,
    @Schema(description = "일별 데이터")
    val dailyData: List<DailySalesData>
)

// ========== 회차별 판매 현황 ==========

@Schema(description = "회차 정보")
data class ScheduleInfo(
    @Schema(description = "회차 ID", example = "101")
    val id: Long,
    @Schema(description = "공연 ID", example = "1")
    val performanceId: Long,
    @Schema(description = "공연 제목", example = "뮤지컬 위키드")
    val performanceTitle: String,
    @Schema(description = "공연 일시")
    val performanceDate: LocalDateTime,
    @Schema(description = "공연장명", example = "블루스퀘어 신한카드홀")
    val venue: String?
)

@Schema(description = "회차 판매 현황")
data class ScheduleSalesDetail(
    @Schema(description = "총 매출액", example = "2500000")
    val totalRevenue: Long,
    @Schema(description = "총 예매 건수", example = "45")
    val totalBookings: Long,
    @Schema(description = "확정 예매 건수", example = "42")
    val confirmedBookings: Long,
    @Schema(description = "취소 예매 건수", example = "3")
    val cancelledBookings: Long,
    @Schema(description = "총 좌석 수", example = "100")
    val totalSeats: Long,
    @Schema(description = "판매 좌석 수", example = "85")
    val soldSeats: Long,
    @Schema(description = "대기중 좌석 수", example = "5")
    val pendingSeats: Long,
    @Schema(description = "예매 가능 좌석 수", example = "10")
    val availableSeats: Long,
    @Schema(description = "판매율 (%)", example = "85.0")
    val salesRate: Double
)

@Schema(description = "등급별 판매 현황")
data class GradeSalesDetail(
    @Schema(description = "좌석 등급")
    val grade: SeatGrade,
    @Schema(description = "총 좌석 수", example = "20")
    val totalSeats: Long,
    @Schema(description = "판매 좌석 수", example = "18")
    val soldSeats: Long,
    @Schema(description = "대기중 좌석 수", example = "1")
    val pendingSeats: Long,
    @Schema(description = "매출액", example = "1800000")
    val revenue: Long,
    @Schema(description = "판매율 (%)", example = "90.0")
    val salesRate: Double
)

@Schema(description = "회차 판매 현황 응답")
data class ScheduleSalesResponse(
    @Schema(description = "회차 정보")
    val schedule: ScheduleInfo,
    @Schema(description = "판매 현황")
    val sales: ScheduleSalesDetail,
    @Schema(description = "등급별 판매 현황")
    val salesByGrade: List<GradeSalesDetail>,
    @Schema(description = "최근 예매 내역")
    val recentBookings: List<RecentBookingResponse>
)

// ========== 최근 예매 내역 ==========

@Schema(description = "최근 예매 응답")
data class RecentBookingResponse(
    @Schema(description = "예매 ID")
    val bookingId: String,
    @Schema(description = "예매 번호", example = "BK20251225001")
    val bookingNumber: String,
    @Schema(description = "예매 상태", example = "CONFIRMED")
    val status: String,
    @Schema(description = "공연 제목", example = "뮤지컬 위키드")
    val performanceTitle: String,
    @Schema(description = "공연 일시")
    val scheduleDate: LocalDateTime,
    @Schema(description = "예매자명 (마스킹)", example = "홍*동")
    val userName: String,
    @Schema(description = "예매자 이메일 (마스킹)", example = "ho***@email.com")
    val userEmail: String,
    @Schema(description = "티켓 수", example = "2")
    val ticketCount: Int,
    @Schema(description = "총 금액", example = "150000")
    val totalPrice: Int,
    @Schema(description = "예매 일시")
    val createdAt: LocalDateTime
)

@Schema(description = "최근 예매 목록 응답")
data class RecentBookingsResponse(
    @Schema(description = "예매 목록")
    val bookings: List<RecentBookingResponse>
)

// ========== Projection (Repository용) ==========

interface DashboardSummaryProjection {
    val totalRevenue: Long
    val totalBookings: Long
    val confirmedBookings: Long
    val cancelledBookings: Long
    val expiredBookings: Long
    val totalTicketsSold: Long
}

interface PerformanceSalesProjection {
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
    val totalTicketsSold: Long
}

interface DailySalesProjection {
    val date: LocalDate
    val revenue: Long
    val bookings: Long
    val ticketsSold: Long
}

interface GradeSalesProjection {
    val grade: SeatGrade
    val revenue: Long
    val ticketsSold: Long
}

interface ScheduleSalesProjection {
    val scheduleId: Long
    val performanceDate: LocalDateTime
    val totalSeats: Long
    val soldSeats: Long
    val revenue: Long
}
