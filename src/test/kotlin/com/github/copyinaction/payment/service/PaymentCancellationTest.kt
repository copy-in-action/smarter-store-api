package com.github.copyinaction.payment.service

import com.github.copyinaction.booking.domain.Booking
import com.github.copyinaction.booking.domain.BookingStatus
import com.github.copyinaction.booking.dto.BookingResponse
import com.github.copyinaction.booking.service.BookingService
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.coupon.service.CouponService
import com.github.copyinaction.payment.domain.Payment
import com.github.copyinaction.payment.domain.PaymentStatus
import com.github.copyinaction.payment.repository.PaymentRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import java.util.*

class PaymentCancellationTest {
    private val paymentRepository = mockk<PaymentRepository>()
    private val bookingRepository = mockk(relaxed = true)
    private val couponService = mockk<CouponService>(relaxed = true)
    private val salesStatsService = mockk<com.github.copyinaction.stats.service.SalesStatsService>(relaxed = true)
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val bookingService = mockk<BookingService>()

    private lateinit var paymentService: PaymentService

    @BeforeEach
    fun setUp() {
        paymentService = PaymentService(
            paymentRepository,
            bookingRepository,
            couponService,
            salesStatsService,
            eventPublisher,
            bookingService
        )
    }

    @Test
    @DisplayName("cancelPayment는 BookingService.cancelBooking을 호출한다")
    fun cancelPaymentCallsBookingService() {
        val paymentId = UUID.randomUUID()
        val bookingId = UUID.randomUUID()
        val userId = 1L

        val booking = mockk<Booking> {
            every { id } returns bookingId
        }
        val payment = mockk<Payment> {
            every { id } returns paymentId
            every { this@mockk.booking } returns booking
            every { this@mockk.userId } returns userId
        }

        every { paymentRepository.findByIdOrNull(paymentId) } returns payment
        every { bookingService.cancelBooking(bookingId, userId, any()) } returns mockk<BookingResponse>(relaxed = true)
        every { paymentRepository.findByIdOrNull(paymentId) } returns payment

        // Act
        paymentService.cancelPayment(paymentId, userId)

        // Assert
        verify { bookingService.cancelBooking(bookingId, userId, "사용자 결제 취소 요청") }
    }

    @Test
    @DisplayName("다른 사용자의 결제를 취소하려 하면 예외가 발생한다")
    fun cancelPaymentThrowsIfDifferentUser() {
        val paymentId = UUID.randomUUID()
        val userId = 1L

        val payment = mockk<Payment> {
            every { this@mockk.userId } returns 2L // Different user
        }

        every { paymentRepository.findByIdOrNull(paymentId) } returns payment

        // Act & Assert
        val exception = assertThrows<CustomException> {
            paymentService.cancelPayment(paymentId, userId)
        }
        assertThat(exception.errorCode).isEqualTo(ErrorCode.FORBIDDEN)
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
