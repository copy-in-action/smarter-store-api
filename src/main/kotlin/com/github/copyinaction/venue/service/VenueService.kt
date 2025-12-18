package com.github.copyinaction.venue.service

import com.github.copyinaction.venue.domain.Venue
import com.github.copyinaction.venue.domain.VenueSeatCapacity
import com.github.copyinaction.venue.dto.CreateVenueRequest
import com.github.copyinaction.venue.dto.SeatingChartRequest
import com.github.copyinaction.venue.dto.SeatingChartResponse
import com.github.copyinaction.venue.dto.UpdateVenueRequest
import com.github.copyinaction.venue.dto.VenueResponse
import com.github.copyinaction.venue.dto.VenueSeatCapacityResponse
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.venue.repository.VenueRepository
import com.github.copyinaction.venue.repository.VenueSeatCapacityRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VenueService(
    private val venueRepository: VenueRepository,
    private val venueSeatCapacityRepository: VenueSeatCapacityRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun createVenue(request: CreateVenueRequest): VenueResponse {
        val venue = Venue.create(
            name = request.name,
            address = request.address,
            phoneNumber = request.phoneNumber
        )
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
        venue.update(
            name = request.name,
            address = request.address,
            phoneNumber = request.phoneNumber
        )
        return VenueResponse.from(venue)
    }

    @Transactional
    fun deleteVenue(id: Long) {
        val venue = findVenueById(id)
        venueRepository.delete(venue)
    }

    // === 좌석 배치도 관련 ===

    fun getSeatingChart(venueId: Long): SeatingChartResponse {
        val venue = findVenueById(venueId)
        val capacities = venueSeatCapacityRepository.findByVenueId(venueId)
            .map { VenueSeatCapacityResponse.from(it) }

        // DB의 JSON 문자열을 객체로 역직렬화
        val seatingChartObject = venue.seatingChart?.let {
            objectMapper.readValue(it, Any::class.java)
        }

        return SeatingChartResponse(
            venueId = venue.id,
            seatingChart = seatingChartObject,
            seatCapacities = capacities.ifEmpty { null }
        )
    }

    @Transactional
    fun updateSeatingChart(venueId: Long, request: SeatingChartRequest): SeatingChartResponse {
        val venue = findVenueById(venueId)

        // 객체를 JSON 문자열로 직렬화해서 DB에 저장
        val seatingChartJson = objectMapper.writeValueAsString(request.seatingChart)
        venue.updateSeatingChart(seatingChartJson)

        // 좌석 수가 함께 전달된 경우 일괄 저장
        val savedCapacities = request.seatCapacities?.let { capacities ->
            venueSeatCapacityRepository.deleteByVenueId(venueId)
            val seatCapacities = capacities.map { req ->
                VenueSeatCapacity.create(
                    venue = venue,
                    seatGrade = req.seatGrade,
                    capacity = req.capacity
                )
            }
            venueSeatCapacityRepository.saveAll(seatCapacities)
                .map { VenueSeatCapacityResponse.from(it) }
        }

        return SeatingChartResponse(
            venueId = venue.id,
            seatingChart = request.seatingChart,  // 요청 객체 그대로 반환
            seatCapacities = savedCapacities
        )
    }

    private fun findVenueById(id: Long): Venue {
        return venueRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.VENUE_NOT_FOUND) }
    }
}