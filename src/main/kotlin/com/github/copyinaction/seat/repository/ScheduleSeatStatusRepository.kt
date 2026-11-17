package com.github.copyinaction.seat.repository

import com.github.copyinaction.seat.domain.ScheduleSeatStatus
import com.github.copyinaction.seat.domain.SeatStatus
import com.github.copyinaction.venue.domain.SeatGrade
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ScheduleSeatStatusRepository : JpaRepository<ScheduleSeatStatus, Long> {

    /**
     * 회차별 등급별 점유 좌석 수 집계
     */
    @Query("""
        SELECT s.seatGrade, COUNT(s)
        FROM ScheduleSeatStatus s
        WHERE s.schedule.id = :scheduleId
        GROUP BY s.seatGrade
    """)
    fun countByScheduleIdGroupBySeatGrade(
        @Param("scheduleId") scheduleId: Long
    ): List<Array<Any>>

    /**
     * 회차별 모든 좌석 상태 조회
     */
    fun findByScheduleId(scheduleId: Long): List<ScheduleSeatStatus>

    /**
     * 회차별 특정 상태의 좌석 조회
     */
    fun findByScheduleIdAndSeatStatus(scheduleId: Long, seatStatus: SeatStatus): List<ScheduleSeatStatus>

    /**
     * 특정 좌석 조회
     */
    fun findByScheduleIdAndRowNumAndColNum(
        scheduleId: Long,
        rowNum: Int,
        colNum: Int
    ): ScheduleSeatStatus?

    /**
     * 특정 좌석들 조회 (비관적 락)
     */
    @Query("""
        SELECT s FROM ScheduleSeatStatus s
        WHERE s.schedule.id = :scheduleId
        AND (s.rowNum, s.colNum) IN :positions
    """)
    fun findByScheduleIdAndPositions(
        @Param("scheduleId") scheduleId: Long,
        @Param("positions") positions: List<Pair<Int, Int>>
    ): List<ScheduleSeatStatus>

    /**
     * 유저가 점유 중인 좌석 조회
     */
    fun findByScheduleIdAndHeldByAndSeatStatus(
        scheduleId: Long,
        heldBy: Long,
        seatStatus: SeatStatus
    ): List<ScheduleSeatStatus>

    /**
     * 만료된 점유 좌석 삭제
     */
    @Modifying
    @Query("""
        DELETE FROM ScheduleSeatStatus s
        WHERE s.seatStatus = 'PENDING'
        AND s.heldUntil < :now
    """)
    fun deleteExpiredHolds(@Param("now") now: LocalDateTime): Int

    /**
     * 특정 좌석들이 사용 가능한지 확인 (점유/예약 상태가 아닌지)
     */
    @Query("""
        SELECT COUNT(s) FROM ScheduleSeatStatus s
        WHERE s.schedule.id = :scheduleId
        AND s.rowNum = :rowNum
        AND s.colNum = :colNum
    """)
    fun countByScheduleIdAndPosition(
        @Param("scheduleId") scheduleId: Long,
        @Param("rowNum") rowNum: Int,
        @Param("colNum") colNum: Int
    ): Long

    /**
     * 특정 유저들이 점유/예약한 모든 좌석 상태 조회 (데모 계정 초기화용)
     */
    fun findByHeldByIn(userIds: Collection<Long>): List<ScheduleSeatStatus>

    /**
     * 특정 유저들이 점유/예약한 모든 좌석 상태 삭제 (데모 계정 초기화용)
     */
    @Modifying
    @Query("DELETE FROM ScheduleSeatStatus s WHERE s.heldBy IN :userIds")
    fun deleteAllByHeldByIn(@Param("userIds") userIds: Collection<Long>): Int

    /**
     * 기준 시각 이전에 열린 회차에서 특정 유저들이 보유한 좌석 상태 삭제 (데모 봇 이력 정리용)
     */
    @Modifying
    @Query("""
        DELETE FROM ScheduleSeatStatus s
        WHERE s.heldBy IN :userIds
        AND s.schedule.id IN (
            SELECT ps.id FROM PerformanceSchedule ps WHERE ps.showDateTime < :cutoff
        )
    """)
    fun deleteAllByHeldByInAndShowDateTimeBefore(
        @Param("userIds") userIds: Collection<Long>,
        @Param("cutoff") cutoff: LocalDateTime
    ): Int

    /**
     * 회차별 점유/예약 좌석 총 개수
     */
    @Query("SELECT COUNT(s) FROM ScheduleSeatStatus s WHERE s.schedule.id = :scheduleId")
    fun countByScheduleId(@Param("scheduleId") scheduleId: Long): Long

    /**
     * 유저의 점유 좌석 삭제
     */
    @Modifying
    fun deleteByScheduleIdAndHeldByAndSeatStatus(
        scheduleId: Long,
        heldBy: Long,
        seatStatus: SeatStatus
    ): Int
}
