package com.github.copyinaction.repository

import com.github.copyinaction.domain.Venue
import org.springframework.data.jpa.repository.JpaRepository

interface VenueRepository : JpaRepository<Venue, Long>
