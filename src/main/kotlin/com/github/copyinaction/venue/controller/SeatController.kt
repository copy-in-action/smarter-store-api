package com.github.copyinaction.venue.controller

import com.github.copyinaction.common.exception.ErrorResponse
import com.github.copyinaction.venue.dto.*
import com.github.copyinaction.venue.service.SeatService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.net.URI

@Tag(name = "seats", description = "좌석 API - 공연장 좌석 관리 API")
@RestController
@RequestMapping("/api")
class SeatController(
    private val seatService: SeatService
) {

    @Operation(summary = "좌석 일괄 생성", description = "공연장에 좌석을 일괄 생성합니다.\n\n**권한: ADMIN**")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "좌석 일괄 생성 성공"),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 입력 값",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "공연장을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409",
            description = "이미 존재하는 좌석",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/venues/{venueId}/seats/bulk")
    fun bulkCreateSeats(
        @Parameter(description = "공연장 ID") @PathVariable venueId: Long,
        @Valid @RequestBody request: BulkCreateSeatRequest
    ): ResponseEntity<BulkCreateSeatResponse> {
        val result = seatService.bulkCreateSeats(venueId, request)
        return ResponseEntity.created(URI.create("/api/venues/$venueId/seats")).body(result)
    }

    @Operation(summary = "좌석 단일 생성", description = "공연장에 좌석을 생성합니다.\n\n**권한: ADMIN**")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "좌석 생성 성공"),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 입력 값",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "공연장을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409",
            description = "이미 존재하는 좌석",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/venues/{venueId}/seats")
    fun createSeat(
        @Parameter(description = "공연장 ID") @PathVariable venueId: Long,
        @Valid @RequestBody request: CreateSeatRequest
    ): ResponseEntity<SeatResponse> {
        val seat = seatService.createSeat(venueId, request)
        val location = URI.create("/api/venues/$venueId/seats/${seat.id}")
        return ResponseEntity.created(location).body(seat)
    }

    @Operation(summary = "공연장 좌석 목록 조회", description = "공연장의 모든 좌석 정보를 조회합니다. (좌석배치도 렌더링용)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "좌석 목록 조회 성공"),
        ApiResponse(
            responseCode = "404",
            description = "공연장을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping("/venues/{venueId}/seats")
    fun getSeatsByVenue(
        @Parameter(description = "공연장 ID") @PathVariable venueId: Long
    ): ResponseEntity<VenueSeatMapResponse> {
        val seatMap = seatService.getSeatsByVenue(venueId)
        return ResponseEntity.ok(seatMap)
    }

    @Operation(summary = "좌석 상세 조회", description = "특정 좌석의 상세 정보를 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "좌석 조회 성공"),
        ApiResponse(
            responseCode = "404",
            description = "좌석을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping("/venues/{venueId}/seats/{seatId}")
    fun getSeat(
        @Parameter(description = "공연장 ID") @PathVariable venueId: Long,
        @Parameter(description = "좌석 ID") @PathVariable seatId: Long
    ): ResponseEntity<SeatResponse> {
        val seat = seatService.getSeat(venueId, seatId)
        return ResponseEntity.ok(seat)
    }

    @Operation(summary = "좌석 수정", description = "좌석 정보를 수정합니다.\n\n**권한: ADMIN**")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "좌석 수정 성공"),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 입력 값",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "좌석을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409",
            description = "이미 존재하는 좌석",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/venues/{venueId}/seats/{seatId}")
    fun updateSeat(
        @Parameter(description = "공연장 ID") @PathVariable venueId: Long,
        @Parameter(description = "좌석 ID") @PathVariable seatId: Long,
        @Valid @RequestBody request: UpdateSeatRequest
    ): ResponseEntity<SeatResponse> {
        val seat = seatService.updateSeat(venueId, seatId, request)
        return ResponseEntity.ok(seat)
    }

    @Operation(summary = "좌석 삭제", description = "좌석을 삭제합니다.\n\n**권한: ADMIN**")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "좌석 삭제 성공"),
        ApiResponse(
            responseCode = "404",
            description = "좌석을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/venues/{venueId}/seats/{seatId}")
    fun deleteSeat(
        @Parameter(description = "공연장 ID") @PathVariable venueId: Long,
        @Parameter(description = "좌석 ID") @PathVariable seatId: Long
    ): ResponseEntity<Unit> {
        seatService.deleteSeat(venueId, seatId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "공연장 전체 좌석 삭제", description = "공연장의 모든 좌석을 삭제합니다.\n\n**권한: ADMIN**")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "좌석 삭제 성공"),
        ApiResponse(
            responseCode = "404",
            description = "공연장을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/venues/{venueId}/seats")
    fun deleteAllSeatsByVenue(
        @Parameter(description = "공연장 ID") @PathVariable venueId: Long
    ): ResponseEntity<Unit> {
        seatService.deleteAllSeatsByVenue(venueId)
        return ResponseEntity.noContent().build()
    }
}
