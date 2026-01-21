package com.github.copyinaction.payment.domain

import com.github.copyinaction.booking.domain.Booking
import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.discount.domain.PaymentDiscount
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Entity
@Table(name = "payments")
class Payment(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    val booking: Booking,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false, unique = true, length = 50)
    val paymentNumber: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var paymentMethod: PaymentMethod,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var paymentStatus: PaymentStatus = PaymentStatus.PENDING,

    @Column(nullable = false)
    val originalPrice: Int,

    @Column(nullable = false)
    var discountAmount: Int = 0,

    @Column(nullable = false)
    val bookingFee: Int = 0,

    @Column(nullable = false)
    var finalPrice: Int,

    @Column(length = 50)
    var pgProvider: String? = null,

    @Column(length = 100)
    var pgTransactionId: String? = null,

    @Column(length = 50)
    var cardCompany: String? = null,

    @Column(length = 20)
    var cardNumberMasked: String? = null,

    @Column
    var installmentMonths: Int? = null,

    @Column(nullable = false)
    val requestedAt: LocalDateTime = LocalDateTime.now(),

    @Column
    var completedAt: LocalDateTime? = null,

    @Column
    var cancelledAt: LocalDateTime? = null,

    @Column
    var refundedAt: LocalDateTime? = null,

    @Column(length = 500)
    var cancelReason: String? = null,

    @Column
    var refundAmount: Int? = null,

    @OneToMany(mappedBy = "payment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val paymentItems: MutableList<PaymentItem> = mutableListOf(),

    @OneToMany(mappedBy = "payment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val discounts: MutableList<PaymentDiscount> = mutableListOf()

) : BaseEntity() {

    companion object {
        fun create(
            booking: Booking,
            userId: Long,
            paymentMethod: PaymentMethod,
            originalPrice: Int,
            bookingFee: Int = 0
        ): Payment {
            val paymentNumber = generatePaymentNumber()
            return Payment(
                booking = booking,
                userId = userId,
                paymentMethod = paymentMethod,
                paymentNumber = paymentNumber,
                originalPrice = originalPrice,
                bookingFee = bookingFee,
                finalPrice = originalPrice + bookingFee
            )
        }

        private fun generatePaymentNumber(): String {
            val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            val random = (100000..999999).random()
            return "PYM-$date-$random"
        }
    }

    fun complete(pgProvider: String, pgTransactionId: String) {
        if (paymentStatus == PaymentStatus.COMPLETED) {
            throw CustomException(ErrorCode.PAYMENT_ALREADY_COMPLETED)
        }
        if (paymentStatus != PaymentStatus.PENDING) {
            throw CustomException(
                ErrorCode.INVALID_INPUT_VALUE,
                "결제 대기 상태에서만 완료 처리가 가능합니다. (현재: $paymentStatus)"
            )
        }
        this.paymentStatus = PaymentStatus.COMPLETED
        this.pgProvider = pgProvider
        this.pgTransactionId = pgTransactionId
        this.completedAt = LocalDateTime.now()
    }

    fun fail(reason: String) {
        this.paymentStatus = PaymentStatus.FAILED
        this.cancelReason = reason
    }

    fun cancel(reason: String) {
        check(paymentStatus == PaymentStatus.COMPLETED) { "결제 완료 상태에서만 취소가 가능합니다." }
        this.paymentStatus = PaymentStatus.CANCELLED
        this.cancelReason = reason
        this.cancelledAt = LocalDateTime.now()
    }

    fun refund(amount: Int, reason: String) {
        check(paymentStatus == PaymentStatus.COMPLETED || paymentStatus == PaymentStatus.PARTIAL_REFUNDED) { 
            "결제 완료 또는 부분 환불 상태에서만 환불이 가능합니다." 
        }
        val currentRefundAmount = (refundAmount ?: 0) + amount
        check(currentRefundAmount <= finalPrice) { "환불 금액이 최종 결제 금액을 초과할 수 없습니다." }

        this.paymentStatus = if (currentRefundAmount == finalPrice)
            PaymentStatus.REFUNDED else PaymentStatus.PARTIAL_REFUNDED
        this.refundAmount = currentRefundAmount
        this.cancelReason = reason
        this.refundedAt = LocalDateTime.now()
    }

    fun addPaymentItem(item: PaymentItem) {
        this.paymentItems.add(item)
    }

    fun addDiscount(discount: PaymentDiscount) {
        this.discounts.add(discount)
        this.discountAmount += discount.discountAmount
        this.finalPrice = originalPrice + bookingFee - discountAmount
    }

    fun validateAmount(expectedAmount: Int) {
        if (this.finalPrice != expectedAmount) {
            throw CustomException(
                ErrorCode.INVALID_INPUT_VALUE,
                "결제 금액이 일치하지 않습니다. (서버 계산: $finalPrice, 요청: $expectedAmount, 원가: $originalPrice, 수수료: $bookingFee, 할인: $discountAmount)"
            )
        }
    }

    fun update(paymentMethod: PaymentMethod) {
        if (paymentStatus == PaymentStatus.COMPLETED) {
            throw CustomException(ErrorCode.PAYMENT_ALREADY_COMPLETED)
        }
        
        this.paymentMethod = paymentMethod
        this.paymentItems.clear()
        this.discounts.clear()
        this.discountAmount = 0
        this.finalPrice = originalPrice + bookingFee
        this.paymentStatus = PaymentStatus.PENDING // 실패 상태였을 수도 있으므로 대기로 변경
    }
}
