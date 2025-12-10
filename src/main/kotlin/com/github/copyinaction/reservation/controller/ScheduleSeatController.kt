package com.github.copyinaction.reservation.controller

import com.github.copyinaction.common.exception.ErrorResponse
import com.github.copyinaction.reservation.dto.*
import com.github.copyinaction.reservation.service.ScheduleSeatService
import com.github.copyinaction.reservation.service.SeatHoldService
import com.github.copyinaction.reservation.service.SeatReservationService
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
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.net.URI

@Tag(name = "schedule-seats", description = "회차별 좌석 API - 좌석 조회/점유/예매 API")
@RestController
@RequestMapping("/api")
class ScheduleSeatController(
    private val scheduleSeatService: ScheduleSeatService,
    private val seatHoldService: SeatHoldService,
    private val seatReservationService: SeatReservationService
) {

    @Operation(summary = "회차별 좌석 초기화", description = "공연 회차에 좌석을 초기화합니다.\n\n**권한: ADMIN**")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "좌석 초기화 성공"),
        ApiResponse(
            responseCode = "404",
            description = "회차/공연장/좌석을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409",
            description = "이미 초기화된 회차",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/schedules/{scheduleId}/seats/initialize")
    fun initializeScheduleSeats(
        @Parameter(description = "회차 ID") @PathVariable scheduleId: Long,
        @Valid @RequestBody request: InitializeScheduleSeatsRequest
    ): ResponseEntity<ScheduleSeatMapResponse> {
        val result = scheduleSeatService.initializeScheduleSeats(scheduleId, request)
        return ResponseEntity.created(URI.create("/api/schedules/$scheduleId/seats")).body(result)
    }

    @Operation(summary = "회차별 좌석 상태 조회", description = "공연 회차의 모든 좌석 상태를 조회합니다. (좌석배치도 렌더링용)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "좌석 상태 조회 성공"),
        ApiResponse(
            responseCode = "404",
            description = "회차를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping("/schedules/{scheduleId}/seats")
    fun getScheduleSeats(
        @Parameter(description = "회차 ID") @PathVariable scheduleId: Long,
        @Parameter(description = "세션 ID (비회원용)") @RequestParam(required = false) sessionId: String?,
        @AuthenticationPrincipal userDetails: UserDetails?
    ): ResponseEntity<ScheduleSeatMapResponse> {
        val userId = userDetails?.username?.toLongOrNull()
        val result = scheduleSeatService.getScheduleSeats(scheduleId, userId, sessionId)
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "회차별 좌석 상태 요약", description = "공연 회차의 좌석 상태별 수량을 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "좌석 요약 조회 성공"),
        ApiResponse(
            responseCode = "404",
            description = "회차를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping("/schedules/{scheduleId}/seats/summary")
    fun getSeatStatusSummary(
        @Parameter(description = "회차 ID") @PathVariable scheduleId: Long
    ): ResponseEntity<SeatStatusSummary> {
        val result = scheduleSeatService.getSeatStatusSummary(scheduleId)
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "좌석 점유", description = "좌석을 임시 점유합니다. (최대 4석, 10분)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "좌석 점유 성공"),
        ApiResponse(
            responseCode = "400",
            description = "최대 좌석 수 초과",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "좌석을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409",
            description = "이미 선택된 좌석",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/schedules/{scheduleId}/seats/hold")
    fun holdSeats(
        @Parameter(description = "회차 ID") @PathVariable scheduleId: Long,
        @Valid @RequestBody request: HoldSeatsRequest,
        @AuthenticationPrincipal userDetails: UserDetails?
    ): ResponseEntity<HoldSeatsResponse> {
        val userId = userDetails?.username?.toLongOrNull()
        val result = seatHoldService.holdSeats(scheduleId, request, userId)
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "좌석 점유 해제", description = "점유한 좌석을 해제합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "좌석 해제 성공"),
        ApiResponse(
            responseCode = "404",
            description = "좌석을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/schedules/{scheduleId}/seats/release")
    fun releaseSeats(
        @Parameter(description = "회차 ID") @PathVariable scheduleId: Long,
        @Valid @RequestBody request: ReleaseSeatsRequest,
        @AuthenticationPrincipal userDetails: UserDetails?
    ): ResponseEntity<ReleaseSeatsResponse> {
        val userId = userDetails?.username?.toLongOrNull()
        val result = seatHoldService.releaseSeats(scheduleId, request, userId)
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "내 점유 좌석 조회", description = "현재 점유 중인 좌석 목록을 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "점유 좌석 조회 성공")
    )
    @GetMapping("/schedules/{scheduleId}/seats/my-holds")
    fun getMyHeldSeats(
        @Parameter(description = "회차 ID") @PathVariable scheduleId: Long,
        @Parameter(description = "세션 ID (비회원용)") @RequestParam(required = false) sessionId: String?,
        @AuthenticationPrincipal userDetails: UserDetails?
    ): ResponseEntity<List<ScheduleSeatResponse>> {
        val userId = userDetails?.username?.toLongOrNull()
        val result = seatHoldService.getHeldSeats(scheduleId, userId, sessionId)
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "좌석 선택 예매", description = "선택한 좌석으로 예매를 생성합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "예매 생성 성공"),
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
            description = "이미 선택된 좌석",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/reservations/seats")
    fun createSeatReservation(
        @Valid @RequestBody request: CreateSeatReservationRequest,
        @AuthenticationPrincipal userDetails: UserDetails?
    ): ResponseEntity<SeatReservationResponse> {
        val userId = userDetails?.username?.toLongOrNull()
        val result = seatReservationService.createSeatReservation(request, userId)
        val location = URI.create("/api/reservations/${result.id}")
        return ResponseEntity.created(location).body(result)
    }

    @Operation(summary = "좌석 예매 조회 (비회원)", description = "예매번호와 연락처로 좌석 예매를 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "예매 조회 성공"),
        ApiResponse(
            responseCode = "404",
            description = "예매를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/reservations/seats/lookup")
    fun lookupSeatReservation(
        @Valid @RequestBody request: ReservationLookupRequest
    ): ResponseEntity<SeatReservationResponse> {
        val result = seatReservationService.getSeatReservationByNumberAndPhone(
            request.reservationNumber,
            request.userPhone
        )
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "내 좌석 예매 목록 조회", description = "로그인한 사용자의 좌석 예매 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "예매 목록 조회 성공"),
        ApiResponse(
            responseCode = "401",
            description = "인증 필요",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping("/reservations/seats/my")
    fun getMySeatReservations(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<List<SeatReservationResponse>> {
        val userId = userDetails.username.toLong()
        val result = seatReservationService.getSeatReservationsByUserId(userId)
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "좌석 예매 상세 조회", description = "좌석 예매 상세 정보를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "예매 조회 성공"),
        ApiResponse(
            responseCode = "404",
            description = "예매를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reservations/seats/{id}")
    fun getSeatReservation(
        @Parameter(description = "예매 ID") @PathVariable id: Long
    ): ResponseEntity<SeatReservationResponse> {
        val result = seatReservationService.getSeatReservation(id)
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "좌석 예매 확정", description = "좌석 예매를 확정 처리합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "예매 확정 성공"),
        ApiResponse(
            responseCode = "400",
            description = "확정 불가능한 상태",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "예매를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reservations/seats/{id}/confirm")
    fun confirmSeatReservation(
        @Parameter(description = "예매 ID") @PathVariable id: Long
    ): ResponseEntity<SeatReservationResponse> {
        val result = seatReservationService.confirmSeatReservation(id)
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "좌석 예매 취소", description = "좌석 예매를 취소합니다. 좌석이 다시 AVAILABLE 상태가 됩니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "예매 취소 성공"),
        ApiResponse(
            responseCode = "400",
            description = "이미 취소된 예매",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "예매를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/reservations/seats/{id}/cancel")
    fun cancelSeatReservation(
        @Parameter(description = "예매 ID") @PathVariable id: Long
    ): ResponseEntity<SeatReservationResponse> {
        val result = seatReservationService.cancelSeatReservation(id)
        return ResponseEntity.ok(result)
    }
}
