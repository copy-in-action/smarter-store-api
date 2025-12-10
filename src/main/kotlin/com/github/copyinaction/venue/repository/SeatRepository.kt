package com.github.copyinaction.venue.repository

import com.github.copyinaction.venue.domain.Seat
import com.github.copyinaction.venue.domain.SeatGrade
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SeatRepository : JpaRepository<Seat, Long> {

    fun findByVenueId(venueId: Long): List<Seat>

    fun findByVenueIdAndSection(venueId: Long, section: String): List<Seat>

    fun findByVenueIdAndSeatGrade(venueId: Long, seatGrade: SeatGrade): List<Seat>

    @Query("SELECT DISTINCT s.section FROM Seat s WHERE s.venue.id = :venueId ORDER BY s.section")
    fun findSectionsByVenueId(@Param("venueId") venueId: Long): List<String>

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.venue.id = :venueId")
    fun countByVenueId(@Param("venueId") venueId: Long): Long

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.venue.id = :venueId AND s.seatGrade = :grade")
    fun countByVenueIdAndSeatGrade(@Param("venueId") venueId: Long, @Param("grade") grade: SeatGrade): Long

    fun existsByVenueIdAndRowAndNumber(venueId: Long, row: String, number: Int): Boolean

    @Query("""
        SELECT s FROM Seat s
        WHERE s.venue.id = :venueId
        ORDER BY s.section, s.row, s.number
    """)
    fun findByVenueIdOrderBySectionAndRowAndNumber(@Param("venueId") venueId: Long): List<Seat>
}
