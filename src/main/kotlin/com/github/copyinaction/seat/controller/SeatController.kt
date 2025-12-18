package com.github.copyinaction.seat.controller

import com.github.copyinaction.common.exception.ErrorResponse
import com.github.copyinaction.seat.dto.ScheduleSeatStatusResponse
import com.github.copyinaction.seat.dto.SeatHoldRequest
import com.github.copyinaction.seat.dto.SeatHoldResponse
import com.github.copyinaction.seat.dto.SeatStatusResponse
import com.github.copyinaction.seat.service.SeatService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@Tag(name = "seat", description = "좌석 API - 좌석 상태 조회 및 점유/예약 처리")
@RestController
@RequestMapping("/api/schedules/{scheduleId}")
class SeatController(
    private val seatService: SeatService
) {

    @Operation(
        summary = "회차별 좌석 상태 조회",
        description = "특정 회차의 좌석 상태 목록을 조회합니다. PENDING(점유 중), RESERVED(예약 완료) 상태인 좌석만 반환합니다.\n\n**권한: 인증 불필요**"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "좌석 상태 조회 성공"),
        ApiResponse(
            responseCode = "404",
            description = "회차를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping("/seat-status")
    fun getSeatStatus(
        @Parameter(description = "회차 ID", required = true, example = "1")
        @PathVariable scheduleId: Long
    ): ResponseEntity<ScheduleSeatStatusResponse> {
        val seatStatus = seatService.getSeatStatus(scheduleId)
        return ResponseEntity.ok(seatStatus)
    }

    @Operation(
        summary = "좌석 점유",
        description = "좌석을 점유합니다. 점유 시간은 10분이며, 최대 4석까지 선택 가능합니다.\n\n**권한: USER, ADMIN**"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "좌석 점유 성공"),
        ApiResponse(
            responseCode = "400",
            description = "최대 좌석 수 초과 또는 잘못된 요청",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "회차를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409",
            description = "이미 점유/예약된 좌석",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/seats/hold")
    fun holdSeats(
        @Parameter(description = "회차 ID", required = true, example = "1")
        @PathVariable scheduleId: Long,
        @RequestBody request: SeatHoldRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<SeatHoldResponse> {
        val userId = userDetails.username.toLong()
        val response = seatService.holdSeats(scheduleId, userId, request.seats)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "좌석 점유 해제",
        description = "점유 중인 좌석을 해제합니다.\n\n**권한: USER, ADMIN**"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "좌석 점유 해제 성공"),
        ApiResponse(
            responseCode = "404",
            description = "회차를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/seats/hold")
    fun releaseSeats(
        @Parameter(description = "회차 ID", required = true, example = "1")
        @PathVariable scheduleId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Unit> {
        val userId = userDetails.username.toLong()
        seatService.releaseSeats(scheduleId, userId)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "좌석 예약 확정",
        description = "점유 중인 좌석을 예약 확정합니다. (결제 완료 후 호출)\n\n**권한: USER, ADMIN**"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "좌석 예약 확정 성공"),
        ApiResponse(
            responseCode = "400",
            description = "예약할 좌석이 없거나 점유 시간 만료",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "회차를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/seats/reserve")
    fun reserveSeats(
        @Parameter(description = "회차 ID", required = true, example = "1")
        @PathVariable scheduleId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<List<SeatStatusResponse>> {
        val userId = userDetails.username.toLong()
        val reservedSeats = seatService.reserveSeats(scheduleId, userId)
        return ResponseEntity.ok(reservedSeats)
    }
}
