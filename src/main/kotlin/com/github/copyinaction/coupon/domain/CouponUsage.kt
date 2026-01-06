package com.github.copyinaction.coupon.domain

import com.github.copyinaction.common.domain.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "coupon_usages")
class CouponUsage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val userId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    val coupon: Coupon,

    @Column(columnDefinition = "uuid", nullable = false)
    val paymentId: UUID,

    @Column(nullable = false)
    val bookingItemId: Long,

    @Column(nullable = false)
    val discountAmount: Int,

    @Column(nullable = false)
    val usedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var isRestored: Boolean = false

) : BaseEntity() {

    fun restore() {
        check(!isRestored) { "이미 복구된 쿠폰 사용 내역입니다." }
        this.isRestored = true
    }

    companion object {
        fun create(
            userId: Long,
            coupon: Coupon,
            paymentId: UUID,
            bookingItemId: Long,
            discountAmount: Int
        ): CouponUsage {
            return CouponUsage(
                userId = userId,
                coupon = coupon,
                paymentId = paymentId,
                bookingItemId = bookingItemId,
                discountAmount = discountAmount
            )
        }
    }
}
