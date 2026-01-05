package com.github.copyinaction.coupon.service

import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.coupon.domain.UserCoupon
import com.github.copyinaction.coupon.domain.UserCouponStatus
import com.github.copyinaction.coupon.dto.CouponIssueRequest
import com.github.copyinaction.coupon.dto.CouponValidateRequest
import com.github.copyinaction.coupon.dto.UserCouponResponse
import com.github.copyinaction.coupon.repository.CouponRepository
import com.github.copyinaction.coupon.repository.UserCouponRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class CouponService(
    private val couponRepository: CouponRepository,
    private val userCouponRepository: UserCouponRepository
) {

    /**
     * 쿠폰 발급
     */
    @Transactional
    fun issueCoupon(userId: Long, request: CouponIssueRequest): UserCouponResponse {
        val coupon = couponRepository.findByCode(request.couponCode)
            .orElseThrow { CustomException(ErrorCode.INVALID_INPUT_VALUE) } // 쿠폰 코드 잘못됨

        if (!coupon.isValid()) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE) // 만료되거나 비활성화됨
        }

        // 중복 발급 체크
        if (userCouponRepository.existsByUserIdAndCouponId(userId, coupon.id)) {
            // 이미 발급된 경우 기존 쿠폰 정보를 반환하거나 예외를 던짐
            val existing = userCouponRepository.findByUserIdAndCouponId(userId, coupon.id)
            return UserCouponResponse.from(existing)
        }

        val userCoupon = UserCoupon(
            userId = userId,
            coupon = coupon
        )

        return try {
            val saved = userCouponRepository.save(userCoupon)
            UserCouponResponse.from(saved)
        } catch (e: DataIntegrityViolationException) {
            // 레이스 컨디션으로 인한 중복 저장 시도 처리
            val existing = userCouponRepository.findByUserIdAndCouponId(userId, coupon.id)
            UserCouponResponse.from(existing)
        }
    }

    /**
     * 쿠폰 사용 가능 여부 확인 및 할인 금액 계산
     */
    fun validateCoupon(userId: Long, request: CouponValidateRequest): Int {
        val userCoupon = userCouponRepository.findByUserIdAndCouponCode(userId, request.couponCode)
            .orElseThrow { CustomException(ErrorCode.INVALID_INPUT_VALUE) }

        if (userCoupon.status != UserCouponStatus.AVAILABLE) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE)
        }

        if (!userCoupon.coupon.isValid()) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE)
        }

        return userCoupon.coupon.calculateDiscount(request.orderAmount)
    }

    /**
     * 사용자 보유 쿠폰 목록 조회
     */
    fun getMyCoupons(userId: Long, availableOnly: Boolean): List<UserCouponResponse> {
        val coupons = if (availableOnly) {
            userCouponRepository.findByUserIdAndStatus(userId, UserCouponStatus.AVAILABLE)
        } else {
            userCouponRepository.findByUserId(userId)
        }
        return coupons.map { UserCouponResponse.from(it) }
    }

    /**
     * 쿠폰 사용 처리 (PaymentService에서 호출)
     */
    @Transactional
    fun useCoupon(userId: Long, couponCode: String, paymentId: UUID, orderAmount: Int) {
        val userCoupon = userCouponRepository.findByUserIdAndCouponCode(userId, couponCode)
            .orElseThrow { CustomException(ErrorCode.INVALID_INPUT_VALUE) }

        userCoupon.use(paymentId, orderAmount)
        userCouponRepository.save(userCoupon)
    }

    /**
     * 쿠폰 복구 처리 (결제 취소 시 호출)
     */
    @Transactional
    fun restoreCoupon(userId: Long, paymentId: UUID) {
        val userCoupon = userCouponRepository.findByUsedPaymentId(paymentId)
            .filter { it.userId == userId }
            .orElse(null) ?: return // 사용된 쿠폰이 없으면 무시

        userCoupon.restore()
        userCouponRepository.save(userCoupon)
    }
}
