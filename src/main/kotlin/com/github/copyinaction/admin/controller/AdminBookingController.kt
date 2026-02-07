package com.github.copyinaction.admin.controller

import com.github.copyinaction.audit.annotation.Auditable
import com.github.copyinaction.audit.domain.AuditAction
import com.github.copyinaction.audit.domain.AuditTargetType
import com.github.copyinaction.booking.dto.AdminBookingResponse
import com.github.copyinaction.booking.dto.BookingDetailResponse
import com.github.copyinaction.booking.dto.BookingResponse
import com.github.copyinaction.booking.service.BookingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@Tag(name = "admin-booking", description = "관리자용 예매 관리 API")
@RestController
@RequestMapping("/api/admin/bookings")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
class AdminBookingController(
    private val bookingService: BookingService
) {

    @Operation(summary = "공연 회차별 예매 목록 조회", description = "특정 공연 회차의 모든 예매 내역을 조회합니다.\n\n**권한: ADMIN**")
    @GetMapping("/schedules/{scheduleId}")
    fun getScheduleBookings(
        @Parameter(description = "회차 ID", required = true, example = "1") @PathVariable scheduleId: Long
    ): ResponseEntity<List<AdminBookingResponse>> {
        val response = bookingService.getScheduleBookingsForAdmin(scheduleId)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "예매 상세 조회", description = "특정 예매 건의 상세 내역과 결제 정보를 조회합니다.\n\n**권한: ADMIN**")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "404", description = "예매 정보를 찾을 수 없음")
    )
    @GetMapping("/{bookingId}")
    fun getBookingDetail(
        @Parameter(description = "예매 ID", required = true) @PathVariable bookingId: UUID
    ): ResponseEntity<BookingDetailResponse> {
        val response = bookingService.getBookingDetailForAdmin(bookingId)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "예매 강제 취소", description = "관리자 권한으로 특정 예매를 강제 취소하고 환불 처리합니다.\n\n**권한: ADMIN**\n\n**[Audit Log]** 이 작업은 감사 로그에 기록됩니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "취소 성공"),
        ApiResponse(responseCode = "404", description = "예매 정보를 찾을 수 없음")
    )
    @DeleteMapping("/{bookingId}")
    @Auditable(
        action = AuditAction.BOOKING_CANCEL,
        targetType = AuditTargetType.BOOKING,
        targetIdParam = "bookingId"
    )
    fun cancelBooking(
        @Parameter(description = "예매 ID", required = true) @PathVariable bookingId: UUID,
        @RequestParam(required = false) cancelReason: String?
    ): ResponseEntity<BookingResponse> {
        val response = bookingService.cancelBookingByAdmin(bookingId, cancelReason)
        return ResponseEntity.ok(response)
    }
}
