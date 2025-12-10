package com.github.copyinaction.reservation.repository

import com.github.copyinaction.reservation.domain.ScheduleSeat
import com.github.copyinaction.reservation.domain.SeatStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ScheduleSeatRepository : JpaRepository<ScheduleSeat, Long> {

    // 비관적 락 - 단일 좌석
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ScheduleSeat s WHERE s.id = :id")
    fun findByIdWithLock(@Param("id") id: Long): ScheduleSeat?

    // 비관적 락 - 다중 좌석 (ID 순 정렬로 데드락 방지)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ScheduleSeat s WHERE s.id IN :ids ORDER BY s.id ASC")
    fun findByIdsWithLock(@Param("ids") ids: List<Long>): List<ScheduleSeat>

    // 회차별 좌석 목록 조회 (좌석 정보 포함)
    @Query("""
        SELECT ss FROM ScheduleSeat ss
        JOIN FETCH ss.seat s
        JOIN FETCH ss.ticketOption t
        WHERE ss.schedule.id = :scheduleId
        ORDER BY s.section, s.row, s.number
    """)
    fun findByScheduleIdWithSeatAndTicketOption(@Param("scheduleId") scheduleId: Long): List<ScheduleSeat>

    // 회차별 특정 상태 좌석 조회
    fun findByScheduleIdAndStatus(scheduleId: Long, status: SeatStatus): List<ScheduleSeat>

    // 만료된 점유 좌석 조회 (스케줄러용)
    @Query("""
        SELECT s FROM ScheduleSeat s
        WHERE s.status = 'HELD'
        AND s.heldUntil < :now
    """)
    fun findExpiredHolds(@Param("now") now: LocalDateTime): List<ScheduleSeat>

    // 사용자가 점유 중인 좌석 조회
    @Query("""
        SELECT s FROM ScheduleSeat s
        WHERE s.schedule.id = :scheduleId
        AND s.status = 'HELD'
        AND (s.heldByUserId = :userId OR s.heldBySessionId = :sessionId)
    """)
    fun findHeldSeatsByUser(
        @Param("scheduleId") scheduleId: Long,
        @Param("userId") userId: Long?,
        @Param("sessionId") sessionId: String?
    ): List<ScheduleSeat>

    // 회차별 좌석 수 통계
    @Query("""
        SELECT s.status, COUNT(s) FROM ScheduleSeat s
        WHERE s.schedule.id = :scheduleId
        GROUP BY s.status
    """)
    fun countByScheduleIdGroupByStatus(@Param("scheduleId") scheduleId: Long): List<Array<Any>>

    // 회차에 좌석이 존재하는지 확인
    fun existsByScheduleId(scheduleId: Long): Boolean

    // 회차별 좌석 삭제 (초기화용)
    fun deleteByScheduleId(scheduleId: Long)
}
