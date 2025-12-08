package com.github.copyinaction.venue.repository

import com.github.copyinaction.venue.domain.Venue
import org.springframework.data.jpa.repository.JpaRepository

interface VenueRepository : JpaRepository<Venue, Long>