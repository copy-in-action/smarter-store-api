package com.github.copyinaction.discount.domain

import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.payment.domain.Payment
import jakarta.persistence.*

@Entity
@Table(name = "payment_discounts")
class PaymentDiscount(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    val payment: Payment,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val discountType: DiscountType,

    @Column(nullable = false, length = 100)
    val discountName: String, // 예: "신규 가입 쿠폰", "국가유공자 할인"

    @Column(nullable = false)
    val discountAmount: Int,

    // 쿠폰인 경우 참조 ID (nullable)
    @Column
    val couponId: Long? = null

) : BaseEntity() {

    companion object {
        fun create(
            payment: Payment,
            type: DiscountType,
            name: String,
            amount: Int,
            couponId: Long? = null
        ): PaymentDiscount {
            return PaymentDiscount(
                payment = payment,
                discountType = type,
                discountName = name,
                discountAmount = amount,
                couponId = couponId
            )
        }
    }
}
