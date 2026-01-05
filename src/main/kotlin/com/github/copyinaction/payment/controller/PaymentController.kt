package com.github.copyinaction.payment.controller

import com.github.copyinaction.auth.service.CustomUserDetails
import com.github.copyinaction.payment.dto.*
import com.github.copyinaction.payment.service.PaymentService
import com.github.copyinaction.common.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
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

@Tag(name = "payment", description = "결제 관련 API")
@RestController
@RequestMapping("/api/payments")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
class PaymentController(
    private val paymentService: PaymentService
) {

    @Operation(summary = "결제 요청 생성", description = "예매 정보를 기반으로 새로운 결제 요청을 생성합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "결제 요청 생성 성공"),
        ApiResponse(responseCode = "404", description = "예매 정보를 찾을 수 없음", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    )
    @PostMapping
    fun createPayment(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody request: PaymentCreateRequest
    ): ResponseEntity<PaymentResponse> {
        val response = paymentService.createPayment(userDetails.id, request)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "결제 완료 승인", description = "PG사 결제 완료 후 승인 처리를 수행합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "결제 승인 성공"),
        ApiResponse(responseCode = "404", description = "결제 정보를 찾을 수 없음", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    )
    @PostMapping("/{id}/complete")
    fun completePayment(
        @PathVariable id: UUID,
        @Valid @RequestBody request: PaymentCompleteRequest
    ): ResponseEntity<PaymentResponse> {
        val response = paymentService.completePayment(id, request)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "결제 취소", description = "결제 완료된 건에 대해 취소를 요청합니다.")
    @PostMapping("/{id}/cancel")
    fun cancelPayment(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable id: UUID,
        @Valid @RequestBody request: PaymentCancelRequest
    ): ResponseEntity<PaymentResponse> {
        val response = paymentService.cancelPayment(id, userDetails.id, request)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "결제 상세 조회", description = "특정 결제 건의 상세 내역과 항목을 조회합니다.")
    @GetMapping("/{id}")
    fun getPayment(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable id: UUID
    ): ResponseEntity<PaymentDetailResponse> {
        val response = paymentService.getPayment(id, userDetails.id)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "내 결제 목록 조회", description = "로그인한 사용자의 전체 결제 내역을 조회합니다.")
    @GetMapping("/me")
    fun getMyPayments(
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<List<PaymentResponse>> {
        val response = paymentService.getPaymentsByUser(userDetails.id)
        return ResponseEntity.ok(response)
    }
}
