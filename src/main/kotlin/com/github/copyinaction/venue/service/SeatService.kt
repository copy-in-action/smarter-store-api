package com.github.copyinaction.venue.service

import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.venue.domain.Seat
import com.github.copyinaction.venue.domain.Venue
import com.github.copyinaction.venue.dto.*
import com.github.copyinaction.venue.repository.SeatRepository
import com.github.copyinaction.venue.repository.VenueRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SeatService(
    private val seatRepository: SeatRepository,
    private val venueRepository: VenueRepository
) {

    @Transactional
    fun createSeat(venueId: Long, request: CreateSeatRequest): SeatResponse {
        val venue = findVenueById(venueId)

        if (seatRepository.existsByVenueIdAndRowAndNumber(venueId, request.row, request.number)) {
            throw CustomException(ErrorCode.SEAT_ALREADY_EXISTS)
        }

        val seat = Seat(
            venue = venue,
            section = request.section,
            row = request.row,
            number = request.number,
            seatGrade = request.seatGrade,
            positionX = request.positionX,
            positionY = request.positionY
        )

        val savedSeat = seatRepository.save(seat)
        return SeatResponse.from(savedSeat)
    }

    @Transactional
    fun bulkCreateSeats(venueId: Long, request: BulkCreateSeatRequest): BulkCreateSeatResponse {
        val venue = findVenueById(venueId)

        val seats = request.seats.map { seatRequest ->
            if (seatRepository.existsByVenueIdAndRowAndNumber(venueId, seatRequest.row, seatRequest.number)) {
                throw CustomException(ErrorCode.SEAT_ALREADY_EXISTS)
            }

            Seat(
                venue = venue,
                section = seatRequest.section,
                row = seatRequest.row,
                number = seatRequest.number,
                seatGrade = seatRequest.seatGrade,
                positionX = seatRequest.positionX,
                positionY = seatRequest.positionY
            )
        }

        val savedSeats = seatRepository.saveAll(seats)

        return BulkCreateSeatResponse(
            createdCount = savedSeats.size,
            seats = savedSeats.map { SeatResponse.from(it) }
        )
    }

    fun getSeat(venueId: Long, seatId: Long): SeatResponse {
        val seat = findSeatById(seatId)
        if (seat.venue.id != venueId) {
            throw CustomException(ErrorCode.SEAT_NOT_FOUND)
        }
        return SeatResponse.from(seat)
    }

    fun getSeatsByVenue(venueId: Long): VenueSeatMapResponse {
        val venue = findVenueById(venueId)
        val seats = seatRepository.findByVenueIdOrderBySectionAndRowAndNumber(venueId)

        val sectionMap = seats.groupBy { it.section }
        val sections = sectionMap.map { (sectionName, sectionSeats) ->
            SectionSeatsResponse(
                name = sectionName,
                seats = sectionSeats.map { SeatResponse.from(it) }
            )
        }

        return VenueSeatMapResponse(
            venueId = venue.id,
            venueName = venue.name,
            totalSeats = seats.size,
            sections = sections
        )
    }

    @Transactional
    fun updateSeat(venueId: Long, seatId: Long, request: UpdateSeatRequest): SeatResponse {
        val seat = findSeatById(seatId)
        if (seat.venue.id != venueId) {
            throw CustomException(ErrorCode.SEAT_NOT_FOUND)
        }

        // 같은 열/번호로 변경 시 중복 체크 (자기 자신 제외)
        if (seat.row != request.row || seat.number != request.number) {
            if (seatRepository.existsByVenueIdAndRowAndNumber(venueId, request.row, request.number)) {
                throw CustomException(ErrorCode.SEAT_ALREADY_EXISTS)
            }
        }

        val updatedSeat = Seat(
            id = seat.id,
            venue = seat.venue,
            section = request.section,
            row = request.row,
            number = request.number,
            seatGrade = request.seatGrade,
            positionX = request.positionX,
            positionY = request.positionY
        )

        val savedSeat = seatRepository.save(updatedSeat)
        return SeatResponse.from(savedSeat)
    }

    @Transactional
    fun deleteSeat(venueId: Long, seatId: Long) {
        val seat = findSeatById(seatId)
        if (seat.venue.id != venueId) {
            throw CustomException(ErrorCode.SEAT_NOT_FOUND)
        }
        seatRepository.delete(seat)
    }

    @Transactional
    fun deleteAllSeatsByVenue(venueId: Long) {
        findVenueById(venueId)
        val seats = seatRepository.findByVenueId(venueId)
        seatRepository.deleteAll(seats)
    }

    private fun findVenueById(id: Long): Venue {
        return venueRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.VENUE_NOT_FOUND) }
    }

    private fun findSeatById(id: Long): Seat {
        return seatRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.SEAT_NOT_FOUND) }
    }
}
