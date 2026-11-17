package com.github.copyinaction.coupon.repository

import com.github.copyinaction.coupon.domain.CouponUsage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CouponUsageRepository : JpaRepository<CouponUsage, Long> {

    fun countByUserIdAndCouponIdAndIsRestoredFalse(userId: Long, couponId: Long): Int

    fun findByPaymentId(paymentId: UUID): List<CouponUsage>

    fun findByPaymentIdAndIsRestoredFalse(paymentId: UUID): List<CouponUsage>

    fun findByUserIdAndCouponId(userId: Long, couponId: Long): List<CouponUsage>

    /**
     * 데모 공용 계정 초기화용: 특정 유저들의 쿠폰 사용 내역 일괄 삭제
     */
    @Modifying
    @Query("DELETE FROM CouponUsage c WHERE c.userId IN :userIds")
    fun deleteAllByUserIdIn(@Param("userIds") userIds: Collection<Long>): Int

    /**
     * 데모 공용 계정 초기화용: 특정 결제들에 연결된 쿠폰 사용 내역 일괄 삭제
     */
    @Modifying
    @Query("DELETE FROM CouponUsage c WHERE c.paymentId IN :paymentIds")
    fun deleteAllByPaymentIdIn(@Param("paymentIds") paymentIds: Collection<UUID>): Int
}
