package com.github.copyinaction.coupon.repository

import com.github.copyinaction.coupon.domain.UserCoupon
import com.github.copyinaction.coupon.domain.UserCouponStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserCouponRepository : JpaRepository<UserCoupon, Long> {
    fun findByUserId(userId: Long): List<UserCoupon>
    fun findByUserIdAndCouponCode(userId: Long, couponCode: String): Optional<UserCoupon>
    fun existsByUserIdAndCouponId(userId: Long, couponId: Long): Boolean
    fun findByUserIdAndCouponId(userId: Long, couponId: Long): UserCoupon
    fun findByUserIdAndStatus(userId: Long, status: UserCouponStatus): List<UserCoupon>
    fun findByUsedPaymentId(usedPaymentId: UUID): Optional<UserCoupon>
}
