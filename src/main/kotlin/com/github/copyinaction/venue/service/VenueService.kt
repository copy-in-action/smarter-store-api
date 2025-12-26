package com.github.copyinaction.venue.service

import com.github.copyinaction.venue.domain.Venue
import com.github.copyinaction.venue.domain.VenueSeatCapacity
import com.github.copyinaction.venue.dto.CreateVenueRequest
import com.github.copyinaction.venue.dto.SeatingChartRequest
import com.github.copyinaction.venue.dto.SeatingChartResponse
import com.github.copyinaction.venue.dto.UpdateVenueRequest
import com.github.copyinaction.venue.dto.VenueResponse
import com.github.copyinaction.venue.dto.VenueSeatCapacityResponse
import com.github.copyinaction.venue.util.SeatingChartParser
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.performance.repository.PerformanceRepository
import com.github.copyinaction.performance.repository.PerformanceScheduleRepository
import com.github.copyinaction.performance.repository.TicketOptionRepository
import com.github.copyinaction.venue.repository.VenueRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class VenueService(
    private val venueRepository: VenueRepository,
    private val performanceRepository: PerformanceRepository,
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val ticketOptionRepository: TicketOptionRepository,
    private val objectMapper: ObjectMapper,
    private val seatingChartParser: SeatingChartParser
) {
    private val log = LoggerFactory.getLogger(javaClass)

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

        // 새로운 좌석 정보 객체 생성 (Venue 객체를 전달)
        val newCapacities = request.seatCapacities?.map { req ->
            VenueSeatCapacity.create(
                venue = venue,
                seatGrade = req.seatGrade,
                capacity = req.capacity
            )
        }

        // 도메인 엔티티에게 업데이트 위임 (Aggregate Root를 통한 관리)
        venue.updateSeatingChart(seatingChartJson, newCapacities)

        // 좌석배치도 변경 시 관련 스케줄의 TicketOption.totalQuantity 동기화
        syncTicketOptionTotalQuantity(venueId, seatingChartJson)

        return SeatingChartResponse(
            venueId = venue.id,
            seatingChart = request.seatingChart,
            seatCapacities = venue.seatCapacities.map { VenueSeatCapacityResponse.from(it) }.ifEmpty { null }
        )
    }

    /**
     * 좌석배치도 변경 시 관련 스케줄의 TicketOption.totalQuantity 동기화
     * - 미래 스케줄(공연 시작 전)만 대상
     */
    private fun syncTicketOptionTotalQuantity(venueId: Long, seatingChartJson: String) {
        val now = LocalDateTime.now()
        val futureSchedules = performanceScheduleRepository.findFutureSchedulesByVenueId(venueId, now)

        if (futureSchedules.isEmpty()) {
            log.info("Venue {} 좌석배치도 변경: 동기화할 미래 스케줄 없음", venueId)
            return
        }

        val seatsByGrade = seatingChartParser.countSeatsByGrade(seatingChartJson)

        var updatedCount = 0
        for (schedule in futureSchedules) {
            val ticketOptions = ticketOptionRepository.findByPerformanceScheduleId(schedule.id)
            for (option in ticketOptions) {
                val newTotalQuantity = seatsByGrade[option.seatGrade] ?: 0
                if (option.totalQuantity != newTotalQuantity) {
                    option.totalQuantity = newTotalQuantity
                    updatedCount++
                }
            }
        }

        log.info("Venue {} 좌석배치도 변경: {} 스케줄, {} TicketOption 동기화 완료",
            venueId, futureSchedules.size, updatedCount)
    }

    private fun findVenueById(id: Long): Venue {
        return venueRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.VENUE_NOT_FOUND) }
    }
}