package com.github.copyinaction.coupon.service

import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.coupon.domain.Coupon
import com.github.copyinaction.coupon.domain.CouponUsage
import com.github.copyinaction.coupon.dto.*
import com.github.copyinaction.coupon.repository.CouponRepository
import com.github.copyinaction.coupon.repository.CouponUsageRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional(readOnly = true)
class CouponService(
    private val couponRepository: CouponRepository,
    private val couponUsageRepository: CouponUsageRepository
) {

    // ==================== 관리자용 메서드 ====================

    /**
     * 쿠폰 생성 (관리자)
     */
    @Transactional
    fun createCoupon(request: CouponCreateRequest): CouponResponse {
        val coupon = Coupon.create(
            name = request.name,
            discountRate = request.discountRate,
            validFrom = request.validFrom,
            validUntil = request.validUntil,
            sortOrder = request.sortOrder
        )

        val saved = couponRepository.save(coupon)
        return CouponResponse.from(saved)
    }

    /**
     * 쿠폰 목록 조회 (관리자)
     */
    fun getAllCoupons(): List<CouponResponse> {
        return couponRepository.findAll().sortedBy { it.sortOrder }.map { CouponResponse.from(it) }
    }

    /**
     * 쿠폰 상세 조회 (관리자)
     */
    fun getCoupon(couponId: Long): CouponResponse {
        val coupon = couponRepository.findById(couponId)
            .orElseThrow { CustomException(ErrorCode.RESOURCE_NOT_FOUND) }
        return CouponResponse.from(coupon)
    }

    /**
     * 쿠폰 수정 (관리자)
     */
    @Transactional
    fun updateCoupon(couponId: Long, request: CouponUpdateRequest): CouponResponse {
        val coupon = couponRepository.findById(couponId)
            .orElseThrow { CustomException(ErrorCode.RESOURCE_NOT_FOUND) }

        coupon.update(
            name = request.name,
            discountRate = request.discountRate,
            validFrom = request.validFrom,
            validUntil = request.validUntil,
            sortOrder = request.sortOrder,
            isActive = request.isActive
        )

        return CouponResponse.from(coupon)
    }

    /**
     * 쿠폰 비활성화 (관리자)
     */
    @Transactional
    fun deactivateCoupon(couponId: Long): CouponResponse {
        val coupon = couponRepository.findById(couponId)
            .orElseThrow { CustomException(ErrorCode.RESOURCE_NOT_FOUND) }
        coupon.isActive = false
        return CouponResponse.from(coupon)
    }

    // ==================== 사용자용 메서드 ====================

    /**
     * 사용 가능한 쿠폰 목록 조회
     */
    fun getAvailableCoupons(userId: Long): List<AvailableCouponResponse> {
        val now = LocalDateTime.now()
        val activeCoupons = couponRepository.findAllByIsActiveTrueAndValidUntilAfter(now)

        val couponList = activeCoupons
            .filter { it.isValid() }
            .sortedBy { it.sortOrder }
            .map { AvailableCouponResponse.from(it) }
            .toMutableList()

        // '일반' (쿠폰 미적용) 항목 추가
        val defaultCoupon = AvailableCouponResponse(
            id = 0L,
            name = "일반",
            discountRate = 0,
            validUntil = now.plusYears(99), // 무기한
            sortOrder = 0
        )
        
        // 정렬 순서가 0인 경우를 고려하여 재정렬하거나, 일반 쿠폰을 최상단에 배치
        // 여기서는 일반 쿠폰을 무조건 맨 앞에 둡니다.
        couponList.add(0, defaultCoupon)

        return couponList
    }

    /**
     * 좌석별 쿠폰 적용 검증
     */
    fun validateSeatCoupons(userId: Long, request: CouponValidateRequest): CouponValidateResponse {
        val results = mutableListOf<SeatCouponResult>()

        for (seatCoupon in request.seatCoupons) {
            val result = validateSingleSeatCoupon(seatCoupon)
            results.add(result)
        }

        val totalOriginalPrice = results.sumOf { it.originalPrice }
        val totalDiscountAmount = results.filter { it.isValid }.sumOf { it.discountAmount }
        val totalFinalPrice = totalOriginalPrice - totalDiscountAmount

        return CouponValidateResponse(
            results = results,
            totalOriginalPrice = totalOriginalPrice,
            totalDiscountAmount = totalDiscountAmount,
            totalFinalPrice = totalFinalPrice,
            allValid = results.all { it.isValid }
        )
    }

    private fun validateSingleSeatCoupon(
        seatCoupon: SeatCouponRequest
    ): SeatCouponResult {
        // 일반(미적용) 쿠폰 처리
        if (seatCoupon.couponId == 0L) {
             return SeatCouponResult(
                bookingSeatId = seatCoupon.bookingSeatId,
                couponId = seatCoupon.couponId,
                originalPrice = seatCoupon.originalPrice,
                discountAmount = 0,
                finalPrice = seatCoupon.originalPrice,
                isValid = true
            )
        }

        val coupon = couponRepository.findById(seatCoupon.couponId).orElse(null)

        // 쿠폰 존재 여부 확인
        if (coupon == null) {
            return SeatCouponResult(
                bookingSeatId = seatCoupon.bookingSeatId,
                couponId = seatCoupon.couponId,
                originalPrice = seatCoupon.originalPrice,
                discountAmount = 0,
                finalPrice = seatCoupon.originalPrice,
                isValid = false,
                message = "존재하지 않는 쿠폰입니다."
            )
        }

        // 쿠폰 유효성 확인
        if (!coupon.isValid()) {
            return SeatCouponResult(
                bookingSeatId = seatCoupon.bookingSeatId,
                couponId = seatCoupon.couponId,
                originalPrice = seatCoupon.originalPrice,
                discountAmount = 0,
                finalPrice = seatCoupon.originalPrice,
                isValid = false,
                message = "유효하지 않거나 만료된 쿠폰입니다."
            )
        }

        // 할인금액 계산
        val discountAmount = coupon.calculateDiscount(seatCoupon.originalPrice)

        return SeatCouponResult(
            bookingSeatId = seatCoupon.bookingSeatId,
            couponId = seatCoupon.couponId,
            originalPrice = seatCoupon.originalPrice,
            discountAmount = discountAmount,
            finalPrice = seatCoupon.originalPrice - discountAmount,
            isValid = true
        )
    }

    /**
     * 할인 금액 계산 (단순 계산)
     */
    fun calculateDiscount(couponId: Long, originalPrice: Int): Int {
        if (couponId == 0L) return 0

        val coupon = couponRepository.findById(couponId)
            .orElseThrow { CustomException(ErrorCode.RESOURCE_NOT_FOUND) }
        
        return coupon.calculateDiscount(originalPrice)
    }

    /**
     * 쿠폰 사용 처리 (결제 완료 시)
     */
    @Transactional
    fun useCoupons(userId: Long, paymentId: UUID, seatCoupons: List<SeatCouponRequest>) {
        for (seatCoupon in seatCoupons) {
            if (seatCoupon.couponId == 0L) continue

            val coupon = couponRepository.findById(seatCoupon.couponId)
                .orElseThrow { CustomException(ErrorCode.RESOURCE_NOT_FOUND) }

            val discountAmount = coupon.calculateDiscount(seatCoupon.originalPrice)

            val usage = CouponUsage.create(
                userId = userId,
                coupon = coupon,
                paymentId = paymentId,
                bookingItemId = seatCoupon.bookingSeatId,
                discountAmount = discountAmount
            )

            couponUsageRepository.save(usage)
        }
    }

    /**
     * 쿠폰 복구 (결제 취소 시)
     */
    @Transactional
    fun restoreCoupons(paymentId: UUID) {
        val usages = couponUsageRepository.findByPaymentIdAndIsRestoredFalse(paymentId)
        usages.forEach { it.restore() }
    }
}