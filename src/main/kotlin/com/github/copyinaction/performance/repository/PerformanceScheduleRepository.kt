package com.github.copyinaction.performance.repository

import com.github.copyinaction.performance.domain.PerformanceSchedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.time.LocalDateTime

interface PerformanceScheduleRepository : JpaRepository<PerformanceSchedule, Long> {
    fun findByPerformanceId(performanceId: Long): List<PerformanceSchedule>

    /**
     * 예매 가능한 회차 조회
     * - 티켓 판매가 시작되었고 (saleStartDateTime <= now)
     * - 공연이 아직 시작하지 않은 (showDateTime > now)
     */
    @Query("""
        SELECT ps FROM PerformanceSchedule ps
        WHERE ps.performance.id = :performanceId
        AND ps.saleStartDateTime <= :now
        AND ps.showDateTime > :now
        ORDER BY ps.showDateTime ASC
    """)
    fun findAvailableSchedules(
        @Param("performanceId") performanceId: Long,
        @Param("now") now: LocalDateTime
    ): List<PerformanceSchedule>

    /**
     * 특정 날짜의 예매 가능한 회차 조회
     * - 해당 날짜의 00:00:00 ~ 23:59:59 범위 내 회차
     */
    @Query("""
        SELECT ps FROM PerformanceSchedule ps
        WHERE ps.performance.id = :performanceId
        AND ps.saleStartDateTime <= :now
        AND ps.showDateTime > :now
        AND ps.showDateTime >= :dateStart
        AND ps.showDateTime < :dateEnd
        ORDER BY ps.showDateTime DESC
    """)
    fun findAvailableSchedulesByDate(
        @Param("performanceId") performanceId: Long,
        @Param("now") now: LocalDateTime,
        @Param("dateStart") dateStart: LocalDateTime,
        @Param("dateEnd") dateEnd: LocalDateTime
    ): List<PerformanceSchedule>
}
