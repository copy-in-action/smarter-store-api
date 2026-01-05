package com.github.copyinaction.coupon.controller

import com.github.copyinaction.auth.service.CustomUserDetails
import com.github.copyinaction.common.exception.ErrorResponse
import com.github.copyinaction.coupon.dto.*
import com.github.copyinaction.coupon.service.CouponService
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

@Tag(name = "coupon", description = "쿠폰 관련 API")
@RestController
@RequestMapping("/api/coupons")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
class CouponController(
    private val couponService: CouponService
) {

    @Operation(summary = "사용 가능한 쿠폰 목록 조회", description = "현재 사용 가능한 쿠폰 목록을 조회합니다. 남은 사용 횟수가 0인 쿠폰은 제외됩니다.")
    @GetMapping
    fun getAvailableCoupons(
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): ResponseEntity<List<AvailableCouponResponse>> {
        val response = couponService.getAvailableCoupons(userDetails.id)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "좌석별 쿠폰 적용 검증", description = "좌석별로 적용된 쿠폰의 유효성을 검증하고 할인 금액을 계산합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "검증 완료"),
        ApiResponse(responseCode = "400", description = "잘못된 요청", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    )
    @PostMapping("/validate")
    fun validateCoupons(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody request: CouponValidateRequest
    ): ResponseEntity<CouponValidateResponse> {
        val response = couponService.validateSeatCoupons(userDetails.id, request)
        return ResponseEntity.ok(response)
    }
}
