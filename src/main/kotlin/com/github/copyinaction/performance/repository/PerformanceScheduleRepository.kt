package com.github.copyinaction.performance.repository

import com.github.copyinaction.performance.domain.PerformanceSchedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.time.LocalDateTime

interface PerformanceScheduleRepository : JpaRepository<PerformanceSchedule, Long> {
    fun findByPerformanceId(performanceId: Long): List<PerformanceSchedule>

    fun existsByPerformanceIdAndShowDateTime(performanceId: Long, showDateTime: LocalDateTime): Boolean

    fun existsByPerformanceIdAndShowDateTimeAndIdNot(performanceId: Long, showDateTime: LocalDateTime, id: Long): Boolean

    /**
     * 예매 가능한 회차 조회
     * - 티켓 판매가 시작되었고 (saleStartDateTime <= now)
     * - 공연이 아직 시작하지 않은 (showDateTime > now)
     */
    @Query("""
        SELECT DISTINCT ps FROM PerformanceSchedule ps
        JOIN FETCH ps.performance p
        LEFT JOIN FETCH ps.ticketOptions
        WHERE p.id = :performanceId
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
        SELECT DISTINCT ps FROM PerformanceSchedule ps
        JOIN FETCH ps.performance p
        LEFT JOIN FETCH ps.ticketOptions
        WHERE p.id = :performanceId
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

    /**
     * 데모 샘플 예매 시딩 대상 회차 ID 조회
     * - 노출 중(visible)이고 공연장이 지정된 공연의 [from, to] 구간 회차
     */
    @Query("""
        SELECT ps.id FROM PerformanceSchedule ps
        WHERE ps.performance.visible = true
        AND ps.performance.venue IS NOT NULL
        AND ps.showDateTime >= :from
        AND ps.showDateTime <= :to
    """)
    fun findSeedableScheduleIds(
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime
    ): List<Long>

    /**
     * [from, to] 구간 회차 ID 조회 (데모 점유율 관리용)
     */
    @Query("""
        SELECT ps.id FROM PerformanceSchedule ps
        WHERE ps.showDateTime >= :from AND ps.showDateTime <= :to
    """)
    fun findScheduleIdsBetween(
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime
    ): List<Long>

    /**
     * 특정 공연장의 미래 스케줄 조회 (좌석배치도 동기화용)
     * - 공연이 아직 시작하지 않은 회차만 대상
     */
    @Query("""
        SELECT DISTINCT ps FROM PerformanceSchedule ps
        JOIN FETCH ps.performance p
        JOIN FETCH p.venue v
        WHERE v.id = :venueId
        AND ps.showDateTime > :now
    """)
    fun findFutureSchedulesByVenueId(
        @Param("venueId") venueId: Long,
        @Param("now") now: LocalDateTime
    ): List<PerformanceSchedule>
}
