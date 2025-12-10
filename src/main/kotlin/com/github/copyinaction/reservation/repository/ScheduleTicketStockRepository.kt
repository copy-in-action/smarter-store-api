package com.github.copyinaction.reservation.repository

import com.github.copyinaction.reservation.domain.ScheduleTicketStock
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ScheduleTicketStockRepository : JpaRepository<ScheduleTicketStock, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ScheduleTicketStock s WHERE s.id = :id")
    fun findByIdWithLock(@Param("id") id: Long): ScheduleTicketStock?

    @Query("""
        SELECT s FROM ScheduleTicketStock s
        JOIN FETCH s.schedule
        JOIN FETCH s.ticketOption
        WHERE s.schedule.id = :scheduleId
    """)
    fun findByScheduleIdWithDetails(@Param("scheduleId") scheduleId: Long): List<ScheduleTicketStock>

    @Query("""
        SELECT s FROM ScheduleTicketStock s
        JOIN FETCH s.schedule sc
        JOIN FETCH s.ticketOption
        WHERE sc.performance.id = :performanceId
    """)
    fun findByPerformanceIdWithDetails(@Param("performanceId") performanceId: Long): List<ScheduleTicketStock>

    fun findByScheduleIdAndTicketOptionId(scheduleId: Long, ticketOptionId: Long): ScheduleTicketStock?
}
