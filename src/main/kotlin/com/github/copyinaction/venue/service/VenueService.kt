package com.github.copyinaction.venue.service

import com.github.copyinaction.venue.domain.SeatGrade
import com.github.copyinaction.venue.domain.Venue
import com.github.copyinaction.venue.domain.VenueSeatCapacity
import com.github.copyinaction.venue.dto.CreateVenueRequest
import com.github.copyinaction.venue.dto.SeatingChartResponse
import com.github.copyinaction.venue.dto.UpdateVenueRequest
import com.github.copyinaction.venue.dto.VenueResponse
import com.github.copyinaction.venue.dto.VenueSeatCapacityRequest
import com.github.copyinaction.venue.dto.VenueSeatCapacityResponse
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.venue.repository.VenueRepository
import com.github.copyinaction.venue.repository.VenueSeatCapacityRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VenueService(
    private val venueRepository: VenueRepository,
    private val venueSeatCapacityRepository: VenueSeatCapacityRepository
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
        return SeatingChartResponse(
            venueId = venue.id,
            seatingChart = venue.seatingChart
        )
    }

    @Transactional
    fun updateSeatingChart(venueId: Long, seatingChart: String): SeatingChartResponse {
        val venue = findVenueById(venueId)
        venue.updateSeatingChart(seatingChart)
        return SeatingChartResponse(
            venueId = venue.id,
            seatingChart = venue.seatingChart
        )
    }

    private fun findVenueById(id: Long): Venue {
        return venueRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.VENUE_NOT_FOUND) }
    }

    // === 등급별 좌석 용량 관련 ===

    fun getSeatCapacities(venueId: Long): List<VenueSeatCapacityResponse> {
        findVenueById(venueId) // 존재 확인
        return venueSeatCapacityRepository.findByVenueId(venueId)
            .map { VenueSeatCapacityResponse.from(it) }
    }

    @Transactional
    fun createSeatCapacity(venueId: Long, request: VenueSeatCapacityRequest): VenueSeatCapacityResponse {
        val venue = findVenueById(venueId)

        // 중복 체크
        val existing = venueSeatCapacityRepository.findByVenueIdAndSeatGrade(venueId, request.seatGrade)
        if (existing != null) {
            throw CustomException(ErrorCode.SEAT_CAPACITY_ALREADY_EXISTS)
        }

        val seatCapacity = VenueSeatCapacity.create(
            venue = venue,
            seatGrade = request.seatGrade,
            capacity = request.capacity
        )
        val saved = venueSeatCapacityRepository.save(seatCapacity)
        return VenueSeatCapacityResponse.from(saved)
    }

    @Transactional
    fun createSeatCapacitiesBulk(venueId: Long, requests: List<VenueSeatCapacityRequest>): List<VenueSeatCapacityResponse> {
        val venue = findVenueById(venueId)

        // 기존 데이터 삭제 후 새로 저장
        venueSeatCapacityRepository.deleteByVenueId(venueId)

        val seatCapacities = requests.map { request ->
            VenueSeatCapacity.create(
                venue = venue,
                seatGrade = request.seatGrade,
                capacity = request.capacity
            )
        }
        val saved = venueSeatCapacityRepository.saveAll(seatCapacities)
        return saved.map { VenueSeatCapacityResponse.from(it) }
    }

    @Transactional
    fun deleteSeatCapacity(venueId: Long, seatGrade: SeatGrade) {
        findVenueById(venueId) // 존재 확인
        val seatCapacity = venueSeatCapacityRepository.findByVenueIdAndSeatGrade(venueId, seatGrade)
            ?: throw CustomException(ErrorCode.SEAT_CAPACITY_NOT_FOUND)
        venueSeatCapacityRepository.delete(seatCapacity)
    }
}