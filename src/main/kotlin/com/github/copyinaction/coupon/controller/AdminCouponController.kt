package com.github.copyinaction.coupon.controller

import com.github.copyinaction.common.exception.ErrorResponse
import com.github.copyinaction.coupon.dto.CouponCreateRequest
import com.github.copyinaction.coupon.dto.CouponResponse
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
import org.springframework.web.bind.annotation.*

@Tag(name = "admin-coupon", description = "관리자용 쿠폰 관리 API")
@RestController
@RequestMapping("/api/admin/coupons")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
class AdminCouponController(
    private val couponService: CouponService
) {

    @Operation(summary = "쿠폰 생성", description = "새로운 쿠폰을 생성합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "쿠폰 생성 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 입력값", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    )
    @PostMapping
    fun createCoupon(
        @Valid @RequestBody request: CouponCreateRequest
    ): ResponseEntity<CouponResponse> {
        val response = couponService.createCoupon(request)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "쿠폰 목록 조회", description = "전체 쿠폰 목록을 조회합니다.")
    @GetMapping
    fun getAllCoupons(): ResponseEntity<List<CouponResponse>> {
        val response = couponService.getAllCoupons()
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "쿠폰 상세 조회", description = "특정 쿠폰의 상세 정보를 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "404", description = "쿠폰을 찾을 수 없음", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    )
    @GetMapping("/{id}")
    fun getCoupon(
        @PathVariable id: Long
    ): ResponseEntity<CouponResponse> {
        val response = couponService.getCoupon(id)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "쿠폰 비활성화", description = "쿠폰을 비활성화합니다. 비활성화된 쿠폰은 사용이 불가합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "비활성화 성공"),
        ApiResponse(responseCode = "404", description = "쿠폰을 찾을 수 없음", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    )
    @PatchMapping("/{id}/deactivate")
    fun deactivateCoupon(
        @PathVariable id: Long
    ): ResponseEntity<CouponResponse> {
        val response = couponService.deactivateCoupon(id)
        return ResponseEntity.ok(response)
    }
}
