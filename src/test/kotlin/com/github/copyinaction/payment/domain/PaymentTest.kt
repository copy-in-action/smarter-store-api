package com.github.copyinaction.payment.domain

import com.github.copyinaction.booking.domain.Booking
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.discount.domain.DiscountType
import com.github.copyinaction.discount.domain.PaymentDiscount
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

class PaymentTest {

    @Test
    @DisplayName("결제 정보를 생성할 수 있다")
    fun createPayment() {
        val booking = mockk<Booking>()
        val userId = 1L
        val originalPrice = 50000
        val bookingFee = 1000

        val payment = Payment.create(
            booking = booking,
            userId = userId,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            originalPrice = originalPrice,
            bookingFee = bookingFee
        )

        assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.PENDING)
        assertThat(payment.finalPrice).isEqualTo(originalPrice + bookingFee)
        assertThat(payment.paymentNumber).startsWith("PYM-")
    }

    @Test
    @DisplayName("결제를 완료 처리할 수 있다")
    fun completePayment() {
        val payment = createPendingPayment()

        payment.complete("KAKAOPAY", "TID-12345")

        assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.COMPLETED)
        assertThat(payment.pgProvider).isEqualTo("KAKAOPAY")
        assertThat(payment.pgTransactionId).isEqualTo("TID-12345")
        assertThat(payment.completedAt).isNotNull()
    }

    @Test
    @DisplayName("결제 완료 상태에서만 취소가 가능하다")
    fun cancelPayment() {
        val payment = createPendingPayment()

        assertThatThrownBy { payment.cancel("단순 변심") }
            .isInstanceOf(IllegalStateException::class.java)

        payment.complete("KAKAOPAY", "TID-12345")
        payment.cancel("단순 변심")

        assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.CANCELLED)
        assertThat(payment.cancelReason).isEqualTo("단순 변심")
        assertThat(payment.cancelledAt).isNotNull()
    }

    @Test
    @DisplayName("전액 환불 시 상태가 REFUNDED로 변경된다")
    fun fullRefund() {
        val payment = createPendingPayment()
        payment.complete("KAKAOPAY", "TID-12345")

        payment.refund(51000, "공연 취소")

        assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.REFUNDED)
        assertThat(payment.refundAmount).isEqualTo(51000)
    }

    @Test
    @DisplayName("부분 환불 시 상태가 PARTIAL_REFUNDED로 변경된다")
    fun partialRefund() {
        val payment = createPendingPayment()
        payment.complete("KAKAOPAY", "TID-12345")

        payment.refund(10000, "일부 취소")

        assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.PARTIAL_REFUNDED)
        assertThat(payment.refundAmount).isEqualTo(10000)

        payment.refund(41000, "나머지 취소")
        assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.REFUNDED)
        assertThat(payment.refundAmount).isEqualTo(51000)
    }

    @Test
    @DisplayName("할인을 적용하면 최종 금액이 차감된다")
    fun applyDiscount() {
        val payment = createPendingPayment()
        val initialPrice = payment.finalPrice

        val discount = PaymentDiscount.create(
            payment = payment,
            type = DiscountType.COUPON,
            name = "신규 가입 쿠폰",
            amount = 5000
        )

        payment.addDiscount(discount)

        assertThat(payment.discountAmount).isEqualTo(5000)
        assertThat(payment.finalPrice).isEqualTo(initialPrice - 5000)
        assertThat(payment.discounts).contains(discount)
    }

    @Test
    @DisplayName("최종 금액 검증 시 일치하지 않으면 예외가 발생한다")
    fun validateAmount() {
        val payment = createPendingPayment()
        // originalPrice(50000) + bookingFee(1000) = 51000

        // 일치하는 경우 (성공)
        payment.validateAmount(51000)

        // 불일치하는 경우 (실패)
        assertThatThrownBy { payment.validateAmount(50000) }
            .isInstanceOf(CustomException::class.java)
    }

    private fun createPendingPayment(): Payment {
        return Payment.create(
            booking = mockk(),
            userId = 1L,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            originalPrice = 50000,
            bookingFee = 1000
        )
    }
}
