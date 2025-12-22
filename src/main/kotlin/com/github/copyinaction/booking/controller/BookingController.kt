package com.github.copyinaction.booking.controller

import com.github.copyinaction.auth.service.CustomUserDetails
import com.github.copyinaction.booking.dto.BookingResponse
import com.github.copyinaction.booking.dto.BookingTimeResponse
import com.github.copyinaction.booking.dto.SeatRequest
import com.github.copyinaction.booking.dto.StartBookingRequest
import com.github.copyinaction.booking.service.BookingService
import com.github.copyinaction.common.exception.ErrorResponse
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
import org.springframework.web.bind.annotation.*
import java.util.UUID

@Tag(name = "booking", description = "예매 API")
@RestController
@RequestMapping("/api/bookings")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
class BookingController(
    private val bookingService: BookingService
) {

    @Operation(summary = "예매 시작", description = "좌석 선택 전, 예매를 시작하고 5분 타이머를 가동합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "예매 시작 성공. 기존에 진행 중인 예매가 있으면 해당 예매 정보를 반환합니다."),
        ApiResponse(
            responseCode = "404", description = "공연 회차를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/start")
    fun startBooking(
        @Valid @RequestBody request: StartBookingRequest,
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<BookingResponse> {
        val response = bookingService.startBooking(request.scheduleId, user.id)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "좌석 선택 (단일)", description = "특정 좌석 1개를 선택하여 점유합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "좌석 선택 성공"),
        ApiResponse(
            responseCode = "400", description = "잘못된 요청 (최대 좌석 수 초과 등)",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404", description = "예매를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409", description = "이미 선택된 좌석",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "410", description = "예매 시간 만료",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/{bookingId}/seats")
    fun selectSeat(
        @Parameter(description = "예매 ID", required = true) @PathVariable bookingId: UUID,
        @Valid @RequestBody request: SeatRequest,
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<BookingResponse> {
        val response = bookingService.selectSeat(bookingId, request, user.id)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "좌석 선택 취소 (단일)", description = "선택했던 좌석 1개를 점유 해제합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "좌석 선택 취소 성공"),
        ApiResponse(
            responseCode = "404", description = "예매를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @DeleteMapping("/{bookingId}/seats")
    fun deselectSeat(
        @Parameter(description = "예매 ID", required = true) @PathVariable bookingId: UUID,
        @Valid @RequestBody request: SeatRequest,
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<BookingResponse> {
        val response = bookingService.deselectSeat(bookingId, request, user.id)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "예매 남은 시간 조회", description = "진행 중인 예매의 남은 시간(초)을 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(
            responseCode = "404", description = "예매를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping("/{bookingId}/time")
    fun getRemainingTime(
        @Parameter(description = "예매 ID", required = true) @PathVariable bookingId: UUID,
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<BookingTimeResponse> {
        val response = bookingService.getRemainingTime(bookingId, user.id)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "예매 확정", description = "결제를 완료하고 예매를 최종 확정합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "예매 확정 성공"),
        ApiResponse(
            responseCode = "404", description = "예매를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "410", description = "예매 시간 만료",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/{bookingId}/confirm")
    fun confirmBooking(
        @Parameter(description = "예매 ID", required = true) @PathVariable bookingId: UUID,
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<BookingResponse> {
        val response = bookingService.confirmBooking(bookingId, user.id)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "예매 취소 (전체)", description = "진행 중인 예매 건 전체를 취소합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "예매 취소 성공"),
        ApiResponse(
            responseCode = "404", description = "예매를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @DeleteMapping("/{bookingId}")
    fun cancelBooking(
        @Parameter(description = "예매 ID", required = true) @PathVariable bookingId: UUID,
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<BookingResponse> {
        val response = bookingService.cancelBooking(bookingId, user.id)
        return ResponseEntity.ok(response)
    }
}
