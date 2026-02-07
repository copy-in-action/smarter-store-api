package com.github.copyinaction.booking.controller

import com.github.copyinaction.audit.annotation.Auditable
import com.github.copyinaction.audit.domain.AuditAction
import com.github.copyinaction.audit.domain.AuditTargetType
import com.github.copyinaction.auth.service.CustomUserDetails
import com.github.copyinaction.booking.dto.*
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

    @Operation(
        summary = "예매 시작 (좌석 일괄 점유)",
        description = """
            결제 진입 시점에 선택한 좌석들을 일괄 점유합니다.

            - 최대 4석까지 선택 가능
            - 점유 시간은 2분 (만료 시 자동 해제)
            - 기존 진행 중인 예매가 있으면 자동 취소 후 새 예매 생성
            - 동시에 같은 좌석을 선택한 경우 먼저 요청한 사용자만 성공 (409 Conflict)

            **권한: USER**

            **[Audit Log]** 이 작업은 감사 로그에 기록됩니다.
        """
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "예매 시작 성공"),
        ApiResponse(
            responseCode = "404", description = "공연 회차를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409", description = "좌석 이미 점유됨",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/start")
    @Auditable(
        action = AuditAction.BOOKING_START,
        targetType = AuditTargetType.SCHEDULE,
        includeRequestBody = true
    )
    fun startBooking(
        @Valid @RequestBody request: StartBookingRequest,
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<BookingResponse> {
        val response = bookingService.startBooking(request.scheduleId, request.seats, user.id)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "내 예매 내역 조회", description = "로그인한 사용자의 전체 예매 내역(확정/취소)을 조회합니다.\n\n**권한: USER**")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공")
    )
    @GetMapping("/me")
    fun getMyBookings(
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<List<BookingHistoryResponse>> {
        val response = bookingService.getMyBookings(user.id)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "내 예매 상세 조회", description = "특정 예매 건의 상세 내역과 결제 정보를 조회합니다.\n\n**권한: USER**")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "404", description = "예매 정보를 찾을 수 없음")
    )
    @GetMapping("/{bookingId}")
    fun getBookingDetail(
        @Parameter(description = "예매 ID", required = true) @PathVariable bookingId: UUID,
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<BookingDetailResponse> {
        val response = bookingService.getBookingDetail(bookingId, user.id)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "예매 남은 시간 조회", description = "진행 중인 예매의 남은 시간(초)을 조회합니다.\n\n**권한: USER**")
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

    @Operation(summary = "예매 확정", description = "결제를 완료하고 예매를 최종 확정합니다.\n\n**권한: USER**\n\n**[Audit Log]** 이 작업은 감사 로그에 기록됩니다.")
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
    @Auditable(
        action = AuditAction.BOOKING_CONFIRM,
        targetType = AuditTargetType.BOOKING,
        targetIdParam = "bookingId"
    )
    fun confirmBooking(
        @Parameter(description = "예매 ID", required = true) @PathVariable bookingId: UUID,
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<BookingResponse> {
        val response = bookingService.confirmBooking(bookingId, user.id)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "예매 취소 (전체)", description = "진행 중인 예매 건 전체를 취소합니다.\n\n**권한: USER**\n\n**[Audit Log]** 이 작업은 감사 로그에 기록됩니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "예매 취소 성공"),
        ApiResponse(
            responseCode = "404", description = "예매를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @DeleteMapping("/{bookingId}")
    @Auditable(
        action = AuditAction.BOOKING_CANCEL,
        targetType = AuditTargetType.BOOKING,
        targetIdParam = "bookingId"
    )
    fun cancelBooking(
        @Parameter(description = "예매 ID", required = true) @PathVariable bookingId: UUID,
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<BookingResponse> {
        val response = bookingService.cancelBooking(bookingId, user.id)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "예매 해제 (POST 방식)",
        description = """
            페이지 unload 시 navigator.sendBeacon을 통한 예매 해제용 POST 엔드포인트입니다.

            - sendBeacon은 POST 방식만 지원하므로 별도 엔드포인트 제공
            - 기능은 DELETE /api/bookings/{bookingId}와 동일
            - 진행 중인 예매 건을 취소하고 좌석 점유를 해제합니다.

            **권한: USER**

            **[Audit Log]** 이 작업은 감사 로그에 기록됩니다.
        """
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "예매 해제 성공"),
        ApiResponse(
            responseCode = "404", description = "예매를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/released")
    @Auditable(
        action = AuditAction.BOOKING_CANCEL,
        targetType = AuditTargetType.BOOKING,
        includeRequestBody = true
    )
    fun releaseBooking(
        @Valid @RequestBody request: ReleaseBookingRequest,
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<BookingResponse> {
        val response = bookingService.cancelBooking(request.bookingId, user.id)
        return ResponseEntity.ok(response)
    }
}
