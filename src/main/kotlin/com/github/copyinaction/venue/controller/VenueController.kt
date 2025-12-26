package com.github.copyinaction.venue.controller

import com.github.copyinaction.venue.dto.SeatingChartResponse
import com.github.copyinaction.venue.dto.VenueResponse
import com.github.copyinaction.common.exception.ErrorResponse
import com.github.copyinaction.venue.service.VenueService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "venue", description = "공연장 API - 공연장 조회 API")
@RestController
@RequestMapping("/api/venues")
class VenueController(
    private val venueService: VenueService
) {

    @Operation(summary = "단일 공연장 조회", description = "ID로 특정 공연장의 정보를 조회합니다.\n\n**권한: 누구나**")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "공연장 조회 성공"),
        ApiResponse(
            responseCode = "404",
            description = "공연장을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping("/{id}")
    fun getVenue(@Parameter(description = "조회할 공연장의 ID", required = true, example = "1") @PathVariable id: Long): ResponseEntity<VenueResponse> {
        val venue = venueService.getVenue(id)
        return ResponseEntity.ok(venue)
    }

    @Operation(summary = "모든 공연장 조회", description = "모든 공연장 목록을 조회합니다.\n\n**권한: 누구나**")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "공연장 목록 조회 성공")
    )
    @GetMapping
    fun getAllVenues(): ResponseEntity<List<VenueResponse>> {
        val venues = venueService.getAllVenues()
        return ResponseEntity.ok(venues)
    }

    // === 좌석 배치도 API ===

    @Operation(summary = "좌석 배치도 조회", description = "공연장의 좌석 배치도 JSON을 조회합니다.\n\n**권한: 누구나**")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "좌석 배치도 조회 성공"),
        ApiResponse(
            responseCode = "404",
            description = "공연장을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping("/{id}/seating-chart")
    fun getSeatingChart(
        @Parameter(description = "공연장 ID", required = true, example = "1") @PathVariable id: Long
    ): ResponseEntity<SeatingChartResponse> {
        val seatingChart = venueService.getSeatingChart(id)
        return ResponseEntity.ok(seatingChart)
    }
}