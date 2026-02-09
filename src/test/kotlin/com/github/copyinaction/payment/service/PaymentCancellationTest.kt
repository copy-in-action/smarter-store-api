package com.github.copyinaction.payment.service

import com.github.copyinaction.booking.repository.BookingRepository
import com.github.copyinaction.coupon.service.CouponService
import com.github.copyinaction.payment.domain.Payment
import com.github.copyinaction.payment.domain.PaymentStatus
import com.github.copyinaction.payment.repository.PaymentRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.util.*

class PaymentCancellationTest {
    private val paymentRepository = mockk<PaymentRepository>()
    private val bookingRepository = mockk<BookingRepository>(relaxed = true)
    private val couponService = mockk<CouponService>(relaxed = true)
    private val salesStatsService = mockk<com.github.copyinaction.stats.service.SalesStatsService>(relaxed = true)
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

    private lateinit var paymentService: PaymentService

    @BeforeEach
    fun setUp() {
        paymentService = PaymentService(
            paymentRepository,
            bookingRepository,
            couponService,
            salesStatsService,
            eventPublisher
        )
    }

    @Test
    @DisplayName("cancelPaymentInternal은 결제 상태를 취소로 변경하고 쿠폰을 복구한다")
    fun cancelPaymentInternalUpdatesStatus() {
        val bookingId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()
        val userId = 1L

        val payment = spyk(Payment(
            id = paymentId,
            booking = mockk { every { id } returns bookingId },
            userId = userId,
            paymentNumber = "PYM-123",
            paymentMethod = com.github.copyinaction.payment.domain.PaymentMethod.CREDIT_CARD,
            originalPrice = 10000,
            finalPrice = 10000,
            paymentStatus = PaymentStatus.COMPLETED
        ))

        every { paymentRepository.findByBookingId(bookingId) } returns payment
        every { paymentRepository.save(any()) } answers { firstArg() }

        // Act
        paymentService.cancelPaymentInternal(bookingId, "테스트 취소")

        // Assert
        assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.CANCELLED)
        assertThat(payment.cancelReason).isEqualTo("테스트 취소")
        verify { couponService.restoreCoupons(paymentId) }
        verify { eventPublisher.publishEvent(any<com.github.copyinaction.payment.domain.PaymentCancelledEvent>()) }
    }
}
