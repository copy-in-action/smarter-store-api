package com.github.copyinaction.reservation.repository

import com.github.copyinaction.reservation.domain.Reservation
import com.github.copyinaction.reservation.domain.ReservationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ReservationRepository : JpaRepository<Reservation, Long> {

    fun findByReservationNumber(reservationNumber: String): Reservation?

    fun findByUserId(userId: Long): List<Reservation>

    fun findByUserIdAndStatus(userId: Long, status: ReservationStatus): List<Reservation>

    @Query("""
        SELECT r FROM Reservation r
        JOIN FETCH r.scheduleTicketStock sts
        JOIN FETCH sts.schedule s
        JOIN FETCH sts.ticketOption t
        JOIN FETCH s.performance p
        WHERE r.reservationNumber = :reservationNumber
    """)
    fun findByReservationNumberWithDetails(@Param("reservationNumber") reservationNumber: String): Reservation?

    @Query("""
        SELECT r FROM Reservation r
        JOIN FETCH r.scheduleTicketStock sts
        JOIN FETCH sts.schedule s
        JOIN FETCH sts.ticketOption t
        JOIN FETCH s.performance p
        WHERE r.userId = :userId
        ORDER BY r.reservedAt DESC
    """)
    fun findByUserIdWithDetails(@Param("userId") userId: Long): List<Reservation>

    @Query("""
        SELECT r FROM Reservation r
        JOIN r.scheduleTicketStock sts
        JOIN sts.schedule s
        WHERE s.performance.id = :performanceId
    """)
    fun findByPerformanceId(@Param("performanceId") performanceId: Long): List<Reservation>
}
