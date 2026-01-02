package com.github.copyinaction.venue.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.performance.repository.PerformanceRepository
import com.github.copyinaction.venue.domain.SeatCapacityCommand
import com.github.copyinaction.venue.domain.SeatingChartUpdatedEvent
import com.github.copyinaction.venue.domain.Venue
import com.github.copyinaction.venue.dto.*
import com.github.copyinaction.venue.repository.VenueRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VenueService(
    private val venueRepository: VenueRepository,
    private val performanceRepository: PerformanceRepository,
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
        if (performanceRepository.existsByVenueId(id)) {
            throw CustomException(ErrorCode.VENUE_HAS_PERFORMANCES)
        }
        venueRepository.delete(venue)
    }

    // === 좌석 배치도 관련 ===

    fun getSeatingChart(venueId: Long): SeatingChartResponse {
        val venue = findVenueById(venueId)

        // DB의 JSON 문자열을 객체로 역직렬화
        val seatingChartObject = venue.seatingChart?.let {
            objectMapper.readValue(it, Any::class.java)
        }

        val capacities = venue.seatCapacities.map { VenueSeatCapacityResponse.from(it) }

        return SeatingChartResponse(
            venueId = venue.id,
            seatingChart = seatingChartObject,
            seatCapacities = capacities.ifEmpty { null }
        )
    }

    @Transactional
    fun updateSeatingChart(venueId: Long, request: SeatingChartRequest): SeatingChartResponse {
        val venue = findVenueById(venueId)

        // 객체를 JSON 문자열로 직렬화
        val seatingChartJson = objectMapper.writeValueAsString(request.seatingChart)

        // Command 객체 생성
        val commands = request.seatCapacities?.map { req ->
            SeatCapacityCommand(
                seatGrade = req.seatGrade,
                capacity = req.capacity
            )
        }

        // 도메인 엔티티에게 업데이트 위임 (Aggregate Root를 통한 관리)
        venue.updateSeatingChart(seatingChartJson, commands)

        // 이벤트 등록 (좌석배치도 변경 시 관련 스케줄 동기화)
        venue.registerEvent(SeatingChartUpdatedEvent(venueId, seatingChartJson))
        venueRepository.save(venue)

        return SeatingChartResponse(
            venueId = venue.id,
            seatingChart = request.seatingChart,
            seatCapacities = venue.seatCapacities.map { VenueSeatCapacityResponse.from(it) }.ifEmpty { null }
        )
    }

    private fun findVenueById(id: Long): Venue {
        return venueRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.VENUE_NOT_FOUND) }
    }
}