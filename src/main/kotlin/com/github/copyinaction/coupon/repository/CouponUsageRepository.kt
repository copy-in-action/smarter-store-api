package com.github.copyinaction.coupon.repository

import com.github.copyinaction.coupon.domain.CouponUsage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CouponUsageRepository : JpaRepository<CouponUsage, Long> {

    fun countByUserIdAndCouponIdAndIsRestoredFalse(userId: Long, couponId: Long): Int

    fun findByPaymentId(paymentId: UUID): List<CouponUsage>

    fun findByPaymentIdAndIsRestoredFalse(paymentId: UUID): List<CouponUsage>

    fun findByUserIdAndCouponId(userId: Long, couponId: Long): List<CouponUsage>
}
