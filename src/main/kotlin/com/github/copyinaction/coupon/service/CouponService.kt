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
    private val couponUsageRepository: CouponUsageRepository,
    private val bookingRepository: com.github.copyinaction.booking.repository.BookingRepository
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
            validUntil = request.validUntil
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
            .map { AvailableCouponResponse.from(it) }
    }

    /**
     * 좌석별 쿠폰 적용 검증
     */
    fun validateSeatCoupons(userId: Long, request: CouponValidateRequest): CouponValidateResponse {
        val targetSeatCoupons = if (request.seatCoupons.isNotEmpty()) {
            request.seatCoupons
        } else if (request.couponIds.isNotEmpty()) {
            autoMapCoupons(request.bookingId, request.couponIds)
        } else {
            emptyList()
        }

        val results = mutableListOf<SeatCouponResult>()

        for (seatCoupon in targetSeatCoupons) {
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

    private fun autoMapCoupons(bookingId: UUID, couponIds: List<Long>): List<SeatCouponRequest> {
        val booking = bookingRepository.findById(bookingId)
            .orElseThrow { CustomException(ErrorCode.BOOKING_NOT_FOUND) }

        // 좌석 가격 내림차순 정렬
        val seats = booking.bookingSeats.sortedByDescending { it.price }
        val coupons = couponIds.mapNotNull { couponRepository.findById(it).orElse(null) }

        val result = mutableListOf<SeatCouponRequest>()
        val minSize = minOf(seats.size, coupons.size)

        for (i in 0 until minSize) {
            val seat = seats[i]
            val coupon = coupons[i]
            result.add(SeatCouponRequest(
                bookingSeatId = seat.id,
                couponId = coupon.id,
                originalPrice = seat.price
            ))
        }
        return result
    }

    private fun validateSingleSeatCoupon(
        seatCoupon: SeatCouponRequest
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