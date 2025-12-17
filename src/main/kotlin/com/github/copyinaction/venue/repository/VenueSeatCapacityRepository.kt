package com.github.copyinaction.venue.repository

import com.github.copyinaction.venue.domain.SeatGrade
import com.github.copyinaction.venue.domain.VenueSeatCapacity
import org.springframework.data.jpa.repository.JpaRepository

interface VenueSeatCapacityRepository : JpaRepository<VenueSeatCapacity, Long> {

    fun findByVenueId(venueId: Long): List<VenueSeatCapacity>

    fun findByVenueIdAndSeatGrade(venueId: Long, seatGrade: SeatGrade): VenueSeatCapacity?

    fun deleteByVenueId(venueId: Long)
}
