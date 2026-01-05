package com.github.copyinaction.coupon.domain

import com.github.copyinaction.common.domain.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(
    name = "user_coupons",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "coupon_id"])]
)
class UserCoupon(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val userId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    val coupon: Coupon,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: UserCouponStatus = UserCouponStatus.AVAILABLE,

    @Column(nullable = false)
    val issuedAt: LocalDateTime = LocalDateTime.now(),

    @Column
    var usedAt: LocalDateTime? = null,

    @Column
    var usedPaymentId: UUID? = null

) : BaseEntity() {

    fun use(paymentId: UUID, orderAmount: Int) {
        check(status == UserCouponStatus.AVAILABLE) { "이미 사용되었거나 만료된 쿠폰입니다." }
        check(coupon.isValid()) { "유효하지 않은 쿠폰입니다." }
        
        // 최소 주문 금액 체크 추가
        coupon.minOrderAmount?.let {
            check(orderAmount >= it) { "최소 주문 금액(${it}원)을 만족하지 않습니다." }
        }

        this.status = UserCouponStatus.USED
        this.usedAt = LocalDateTime.now()
        this.usedPaymentId = paymentId
        coupon.use() // 쿠폰 전체 수량 차감 (여기서 동시성 이슈 발생 가능 -> 서비스 레이어 락 필요)
    }

    fun restore() {
        check(status == UserCouponStatus.USED) { "사용되지 않은 쿠폰은 복구할 수 없습니다." }
        this.status = UserCouponStatus.AVAILABLE
        this.usedAt = null
        this.usedPaymentId = null
        // coupon.restore() // 쿠폰 수량 복구 로직 필요하다면 추가
    }
}
