package com.github.copyinaction.seat.repository

import com.github.copyinaction.seat.domain.ScheduleSeatStatus
import com.github.copyinaction.seat.domain.SeatStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ScheduleSeatStatusRepository : JpaRepository<ScheduleSeatStatus, Long> {

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
     * 유저의 점유 좌석 삭제
     */
    @Modifying
    fun deleteByScheduleIdAndHeldByAndSeatStatus(
        scheduleId: Long,
        heldBy: Long,
        seatStatus: SeatStatus
    ): Int
}
