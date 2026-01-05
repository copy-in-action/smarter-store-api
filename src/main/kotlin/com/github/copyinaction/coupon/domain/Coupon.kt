package com.github.copyinaction.coupon.domain

import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.venue.domain.SeatGrade
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "coupons")
class Coupon(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(length = 500)
    val description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val discountMethod: DiscountMethod,

    @Column(nullable = false)
    val discountValue: Int,

    @Column
    val maxDiscountAmount: Int? = null,

    @Column
    val minOrderAmount: Int? = null,

    @Column
    val performanceId: Long? = null,

    @Enumerated(EnumType.STRING)
    val targetSeatGrade: SeatGrade? = null,

    @Column(nullable = false)
    val validFrom: LocalDateTime,

    @Column(nullable = false)
    val validUntil: LocalDateTime,

    @Column
    val maxUsagePerUser: Int? = null,

    @Column(nullable = false)
    var isActive: Boolean = true

) : BaseEntity() {

    fun isValid(): Boolean {
        val now = LocalDateTime.now()
        return isActive &&
               now.isAfter(validFrom) &&
               now.isBefore(validUntil)
    }

    fun calculateDiscount(orderAmount: Int): Int {
        if (!isValid()) return 0
        if (minOrderAmount != null && orderAmount < minOrderAmount) return 0

        val discount = when (discountMethod) {
            DiscountMethod.FIXED -> discountValue
            DiscountMethod.PERCENTAGE -> (orderAmount * discountValue / 100)
        }

        return maxDiscountAmount?.let { minOf(discount, it) } ?: discount
    }

    companion object {
        fun create(
            name: String,
            discountMethod: DiscountMethod,
            discountValue: Int,
            validFrom: LocalDateTime,
            validUntil: LocalDateTime,
            description: String? = null,
            minOrderAmount: Int? = null,
            maxDiscountAmount: Int? = null,
            maxUsagePerUser: Int? = null,
            performanceId: Long? = null,
            targetSeatGrade: SeatGrade? = null
        ): Coupon {
            return Coupon(
                name = name,
                description = description,
                discountMethod = discountMethod,
                discountValue = discountValue,
                validFrom = validFrom,
                validUntil = validUntil,
                minOrderAmount = minOrderAmount,
                maxDiscountAmount = maxDiscountAmount,
                maxUsagePerUser = maxUsagePerUser,
                performanceId = performanceId,
                targetSeatGrade = targetSeatGrade
            )
        }
    }
}
