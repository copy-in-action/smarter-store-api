package com.github.copyinaction.reservation.controller

import com.github.copyinaction.common.exception.ErrorResponse
import com.github.copyinaction.reservation.dto.CreateReservationRequest
import com.github.copyinaction.reservation.dto.ReservationLookupRequest
import com.github.copyinaction.reservation.dto.ReservationResponse
import com.github.copyinaction.reservation.service.ReservationService
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

@Tag(name = "reservations", description = "예매 API")
@RestController
@RequestMapping("/api/reservations")
class ReservationController(
    private val reservationService: ReservationService
) {

    @Operation(summary = "예매 생성", description = "공연 예매를 생성합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "예매 생성 성공"),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 입력 값 또는 잔여석 부족",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "좌석 정보를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping
    fun createReservation(
        @Valid @RequestBody request: CreateReservationRequest,
        @AuthenticationPrincipal userDetails: UserDetails?
    ): ResponseEntity<ReservationResponse> {
        val userId = userDetails?.username?.toLongOrNull()
        val reservation = reservationService.createReservation(request, userId)
        val location = URI.create("/api/reservations/${reservation.id}")
        return ResponseEntity.created(location).body(reservation)
    }

    @Operation(summary = "예매 조회 (비회원)", description = "예매번호와 연락처로 예매를 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "예매 조회 성공"),
        ApiResponse(
            responseCode = "404",
            description = "예매를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/lookup")
    fun lookupReservation(
        @Valid @RequestBody request: ReservationLookupRequest
    ): ResponseEntity<ReservationResponse> {
        val reservation = reservationService.getReservationByNumberAndPhone(
            request.reservationNumber,
            request.userPhone
        )
        return ResponseEntity.ok(reservation)
    }

    @Operation(summary = "내 예매 목록 조회", description = "로그인한 사용자의 예매 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "예매 목록 조회 성공"),
        ApiResponse(
            responseCode = "401",
            description = "인증 필요",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping("/my")
    fun getMyReservations(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<List<ReservationResponse>> {
        val userId = userDetails.username.toLong()
        val reservations = reservationService.getReservationsByUserId(userId)
        return ResponseEntity.ok(reservations)
    }

    @Operation(summary = "예매 상세 조회", description = "예매 ID로 상세 정보를 조회합니다.")
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
    @GetMapping("/{id}")
    fun getReservation(
        @Parameter(description = "예매 ID") @PathVariable id: Long
    ): ResponseEntity<ReservationResponse> {
        val reservation = reservationService.getReservation(id)
        return ResponseEntity.ok(reservation)
    }

    @Operation(summary = "예매 확정", description = "예매를 확정 처리합니다. (결제 완료)")
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
    @PostMapping("/{id}/confirm")
    fun confirmReservation(
        @Parameter(description = "예매 ID") @PathVariable id: Long
    ): ResponseEntity<ReservationResponse> {
        val reservation = reservationService.confirmReservation(id)
        return ResponseEntity.ok(reservation)
    }

    @Operation(summary = "예매 취소", description = "예매를 취소합니다. 잔여석이 복구됩니다.")
    @SecurityRequirement(name = "bearerAuth")
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
    @PostMapping("/{id}/cancel")
    fun cancelReservation(
        @Parameter(description = "예매 ID") @PathVariable id: Long
    ): ResponseEntity<ReservationResponse> {
        val reservation = reservationService.cancelReservation(id)
        return ResponseEntity.ok(reservation)
    }
}
