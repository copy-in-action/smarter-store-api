package com.github.copyinaction.coupon.domain

import com.github.copyinaction.common.domain.BaseEntity
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val discountMethod: DiscountMethod,

    @Column(nullable = false)
    val discountValue: Int,

    @Column(nullable = false)
    val validFrom: LocalDateTime,

    @Column(nullable = false)
    val validUntil: LocalDateTime,

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

        return when (discountMethod) {
            DiscountMethod.FIXED -> discountValue
            DiscountMethod.PERCENTAGE -> (orderAmount * discountValue / 100)
        }
    }

    companion object {
        fun create(
            name: String,
            discountMethod: DiscountMethod,
            discountValue: Int,
            validFrom: LocalDateTime,
            validUntil: LocalDateTime
        ): Coupon {
            return Coupon(
                name = name,
                discountMethod = discountMethod,
                discountValue = discountValue,
                validFrom = validFrom,
                validUntil = validUntil
            )
        }
    }
}
