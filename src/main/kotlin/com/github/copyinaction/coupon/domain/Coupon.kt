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
    var name: String,

    @Column(nullable = false)
    var discountRate: Int,

    @Column(nullable = false)
    var validFrom: LocalDateTime,

    @Column(nullable = false)
    var validUntil: LocalDateTime,

    @Column(nullable = false)
    var sortOrder: Int = 0,

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

        // 정률 할인 계산 (원 단위 절삭 등 정책 필요 시 적용, 여기선 단순 계산)
        return (orderAmount * discountRate / 100)
    }

    fun update(
        name: String,
        discountRate: Int,
        validFrom: LocalDateTime,
        validUntil: LocalDateTime,
        sortOrder: Int,
        isActive: Boolean
    ) {
        this.name = name
        this.discountRate = discountRate
        this.validFrom = validFrom
        this.validUntil = validUntil
        this.sortOrder = sortOrder
        this.isActive = isActive
    }

    companion object {
        fun create(
            name: String,
            discountRate: Int,
            validFrom: LocalDateTime,
            validUntil: LocalDateTime,
            sortOrder: Int = 0
        ): Coupon {
            return Coupon(
                name = name,
                discountRate = discountRate,
                validFrom = validFrom,
                validUntil = validUntil,
                sortOrder = sortOrder
            )
        }
    }
}
