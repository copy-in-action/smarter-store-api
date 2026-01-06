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
            description = request.description,
            discountMethod = request.discountMethod,
            discountValue = request.discountValue,
            validFrom = request.validFrom,
            validUntil = request.validUntil,
            minOrderAmount = request.minOrderAmount,
            maxDiscountAmount = request.maxDiscountAmount,
            maxUsagePerUser = request.maxUsagePerUser
        )

        val saved = couponRepository.save(coupon)
        return CouponResponse.from(saved)
    }

    /**
     * 쿠폰 목록 조회 (관리자)
     */
    fun getAllCoupons(): List<CouponResponse> {
        return couponRepository.findAll().map { CouponResponse.from(it) }
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

        return activeCoupons
            .filter { it.isValid() }
            .map { coupon ->
                val remainingUsage = getRemainingUsage(userId, coupon.id)
                // 사용 횟수가 0이면 목록에서 제외
                if (remainingUsage != null && remainingUsage <= 0) {
                    null
                } else {
                    AvailableCouponResponse.from(coupon, remainingUsage)
                }
            }
            .filterNotNull()
    }

    /**
     * 좌석별 쿠폰 적용 검증
     */
    fun validateSeatCoupons(userId: Long, request: CouponValidateRequest): CouponValidateResponse {
        val results = mutableListOf<SeatCouponResult>()

        // 각 쿠폰별 사용 횟수 추적 (동일 쿠폰 여러 좌석 적용 시)
        val couponUsageCount = mutableMapOf<Long, Int>()

        for (seatCoupon in request.seatCoupons) {
            val result = validateSingleSeatCoupon(userId, seatCoupon, couponUsageCount)
            results.add(result)

            // 유효한 경우 사용 횟수 증가
            if (result.isValid) {
                couponUsageCount[seatCoupon.couponId] =
                    couponUsageCount.getOrDefault(seatCoupon.couponId, 0) + 1
            }
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
        userId: Long,
        seatCoupon: SeatCouponRequest,
        pendingUsageCount: Map<Long, Int>
    ): SeatCouponResult {
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

        // 사용 횟수 제한 확인
        if (coupon.maxUsagePerUser != null) {
            val usedCount = couponUsageRepository.countByUserIdAndCouponIdAndIsRestoredFalse(userId, coupon.id)
            val pendingCount = pendingUsageCount.getOrDefault(coupon.id, 0)
            val totalUsage = usedCount + pendingCount

            if (totalUsage >= coupon.maxUsagePerUser) {
                return SeatCouponResult(
                    bookingSeatId = seatCoupon.bookingSeatId,
                    couponId = seatCoupon.couponId,
                    originalPrice = seatCoupon.originalPrice,
                    discountAmount = 0,
                    finalPrice = seatCoupon.originalPrice,
                    isValid = false,
                    message = "쿠폰 사용 횟수를 초과했습니다."
                )
            }
        }

        // 최소 주문금액 확인
        if (coupon.minOrderAmount != null && seatCoupon.originalPrice < coupon.minOrderAmount) {
            return SeatCouponResult(
                bookingSeatId = seatCoupon.bookingSeatId,
                couponId = seatCoupon.couponId,
                originalPrice = seatCoupon.originalPrice,
                discountAmount = 0,
                finalPrice = seatCoupon.originalPrice,
                isValid = false,
                message = "최소 주문금액(${coupon.minOrderAmount}원)을 충족하지 않습니다."
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
     * 쿠폰 사용 처리 (결제 완료 시)
     */
    @Transactional
    fun useCoupons(userId: Long, paymentId: UUID, seatCoupons: List<SeatCouponRequest>) {
        for (seatCoupon in seatCoupons) {
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

    /**
     * 남은 사용 횟수 조회
     */
    fun getRemainingUsage(userId: Long, couponId: Long): Int? {
        val coupon = couponRepository.findById(couponId).orElse(null) ?: return null

        if (coupon.maxUsagePerUser == null) {
            return null // 무제한
        }

        val usedCount = couponUsageRepository.countByUserIdAndCouponIdAndIsRestoredFalse(userId, couponId)
        return maxOf(0, coupon.maxUsagePerUser - usedCount)
    }
}
