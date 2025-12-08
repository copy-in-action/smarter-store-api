package com.github.copyinaction.service

import com.github.copyinaction.domain.Venue
import com.github.copyinaction.dto.CreateVenueRequest
import com.github.copyinaction.dto.UpdateVenueRequest
import com.github.copyinaction.dto.VenueResponse
import com.github.copyinaction.exception.CustomException
import com.github.copyinaction.exception.ErrorCode
import com.github.copyinaction.repository.VenueRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VenueService(
    private val venueRepository: VenueRepository
) {

    @Transactional
    fun createVenue(request: CreateVenueRequest): VenueResponse {
        val venue = request.toEntity()
        val savedVenue = venueRepository.save(venue)
        return VenueResponse.from(savedVenue)
    }

    fun getVenue(id: Long): VenueResponse {
        val venue = findVenueById(id)
        return VenueResponse.from(venue)
    }

    fun getAllVenues(): List<VenueResponse> {
        return venueRepository.findAll().map { VenueResponse.from(it) }
    }

    @Transactional
    fun updateVenue(id: Long, request: UpdateVenueRequest): VenueResponse {
        val venue = findVenueById(id)
        venue.update(name = request.name, address = request.address, seatingChartUrl = request.seatingChartUrl)
        return VenueResponse.from(venue)
    }

    @Transactional
    fun deleteVenue(id: Long) {
        val venue = findVenueById(id)
        venueRepository.delete(venue)
    }

    private fun findVenueById(id: Long): Venue {
        return venueRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.VENUE_NOT_FOUND) }
    }
}
