package com.github.copyinaction.reservation.repository

import com.github.copyinaction.reservation.domain.ReservationSeat
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ReservationSeatRepository : JpaRepository<ReservationSeat, Long> {

    fun findByReservationId(reservationId: Long): List<ReservationSeat>

    @Query("""
        SELECT rs FROM ReservationSeat rs
        JOIN FETCH rs.scheduleSeat ss
        JOIN FETCH ss.seat s
        JOIN FETCH ss.ticketOption t
        WHERE rs.reservation.id = :reservationId
    """)
    fun findByReservationIdWithDetails(@Param("reservationId") reservationId: Long): List<ReservationSeat>

    fun deleteByReservationId(reservationId: Long)
}
