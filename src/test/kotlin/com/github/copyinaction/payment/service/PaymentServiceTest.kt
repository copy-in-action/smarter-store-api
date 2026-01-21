package com.github.copyinaction.payment.service

import com.github.copyinaction.booking.domain.Booking
import com.github.copyinaction.booking.repository.BookingRepository
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.coupon.service.CouponService
import com.github.copyinaction.discount.domain.DiscountType
import com.github.copyinaction.payment.domain.Payment
import com.github.copyinaction.payment.domain.PaymentMethod
import com.github.copyinaction.payment.domain.PaymentStatus
import com.github.copyinaction.payment.dto.AppliedDiscountDto
import com.github.copyinaction.payment.dto.PaymentCreateRequest
import com.github.copyinaction.payment.repository.PaymentRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime
import java.util.*

class PaymentServiceTest {
    private val paymentRepository = mockk<PaymentRepository>()
    private val bookingRepository = mockk<BookingRepository>()
    private val couponService = mockk<CouponService>(relaxed = true)
    private val salesStatsService = mockk<com.github.copyinaction.stats.service.SalesStatsService>(relaxed = true)
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    
    private val paymentService = PaymentService(
        paymentRepository, 
        bookingRepository, 
        couponService, 
        salesStatsService,
        eventPublisher
    )

    @Test
    @DisplayName("결제 생성 시 쿠폰 할인이 포함되어 있으면 쿠폰 사용 로직이 호출된다")
    fun createPaymentWithCoupon() {
        val bookingId = UUID.randomUUID()
        val userId = 1L
        
        // Mocking
        val siteUser = mockk<com.github.copyinaction.auth.domain.User> {
            every { id } returns userId
        }
        val booking = mockk<Booking> {
            every { id } returns bookingId
            every { this@mockk.siteUser } returns siteUser
        }
        
        // Request with Coupon Discount
        val originalPrice = 50000
        val bookingFee = 1000
        val discountAmount = 5000
        val finalPrice = originalPrice + bookingFee - discountAmount
        
        val discountDto = AppliedDiscountDto(
            type = DiscountType.COUPON,
            name = "Test Coupon",
            amount = discountAmount,
            couponId = 123L
        )
        
        val request = PaymentCreateRequest(
            bookingId = bookingId,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            originalPrice = originalPrice,
            bookingFee = bookingFee,
            totalAmount = finalPrice,
            ticketAmount = originalPrice - discountAmount,
            isAgreed = true,
            discounts = listOf(discountDto)
        )

        every { bookingRepository.findByIdOrNull(bookingId) } returns booking
        every { paymentRepository.findByBookingId(bookingId) } returns null // No existing payment
        every { paymentRepository.save(any()) } answers { firstArg() }
        
        // Mock BookingSeats for PaymentItem generation
        every { booking.bookingSeats } returns mutableListOf()
        every { booking.schedule.performance } returns mockk(relaxed = true)
        every { booking.schedule.showDateTime } returns LocalDateTime.now()
        every { couponService.calculateDiscount(any(), any()) } returns 5000

        // Act
        val response = paymentService.createPayment(userId, request)

        // Assert
        assertThat(response.finalPrice).isEqualTo(finalPrice)
        
        // Verify CouponService call (plural method)
        verify { 
            couponService.useCoupons(
                userId = userId, 
                paymentId = any(), 
                seatCoupons = any()
            ) 
        }
    }

    @Test
    @DisplayName("이미 존재하는 결제가 있으면 재사용하고 쿠폰을 복구한다")
    fun createPaymentReusesExistingPayment() {
        val bookingId = UUID.randomUUID()
        val userId = 1L
        val existingPaymentId = UUID.randomUUID()

        // Mocking
        val siteUser = mockk<com.github.copyinaction.auth.domain.User> {
            every { id } returns userId
        }
        val booking = mockk<Booking> {
            every { id } returns bookingId
            every { this@mockk.siteUser } returns siteUser
        }
        
        // Existing Payment Mock (PENDING state)
        val existingPayment = spyk(Payment(
            id = existingPaymentId,
            booking = booking,
            userId = userId,
            paymentNumber = "PYM-OLD",
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            originalPrice = 50000,
            bookingFee = 1000,
            finalPrice = 51000
        ))
        
        val request = PaymentCreateRequest(
            bookingId = bookingId,
            paymentMethod = PaymentMethod.CREDIT_CARD, // Changed method
            originalPrice = 50000,
            bookingFee = 1000,
            totalAmount = 51000,
            ticketAmount = 50000,
            isAgreed = true,
            discounts = emptyList()
        )

        every { bookingRepository.findByIdOrNull(bookingId) } returns booking
        every { paymentRepository.findByBookingId(bookingId) } returns existingPayment
        every { paymentRepository.save(any()) } answers { firstArg() }
        
        every { booking.bookingSeats } returns mutableListOf()
        every { booking.schedule.performance } returns mockk(relaxed = true)
        every { booking.schedule.showDateTime } returns LocalDateTime.now()

        // Act
        val response = paymentService.createPayment(userId, request)

        // Assert
        assertThat(response.id).isEqualTo(existingPaymentId)
        assertThat(existingPayment.paymentMethod).isEqualTo(PaymentMethod.CREDIT_CARD) // Method updated
        
        verify { couponService.restoreCoupons(existingPaymentId) } // Coupon restoration called
        verify { existingPayment.update(PaymentMethod.CREDIT_CARD) } // Update called
    }

    @Test
    @DisplayName("이미 완료된 결제에 대해 요청하면 예외가 발생한다")
    fun createPaymentThrowsIfCompleted() {
        val bookingId = UUID.randomUUID()
        val userId = 1L
        
        val siteUser = mockk<com.github.copyinaction.auth.domain.User> { every { id } returns userId }
        val booking = mockk<Booking> {
            every { id } returns bookingId
            every { this@mockk.siteUser } returns siteUser
        }
        
        val existingPayment = Payment(
            booking = booking,
            userId = userId,
            paymentNumber = "PYM-COMPLETED",
            paymentMethod = PaymentMethod.CREDIT_CARD,
            originalPrice = 50000,
            bookingFee = 1000,
            finalPrice = 51000,
            paymentStatus = PaymentStatus.COMPLETED // Already Completed
        )

        val request = PaymentCreateRequest(
            bookingId = bookingId,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            originalPrice = 50000,
            bookingFee = 1000,
            totalAmount = 51000,
            ticketAmount = 50000,
            isAgreed = true,
            discounts = emptyList()
        )

        every { bookingRepository.findByIdOrNull(bookingId) } returns booking
        every { paymentRepository.findByBookingId(bookingId) } returns existingPayment

        // Act & Assert
        val exception = assertThrows<CustomException> {
            paymentService.createPayment(userId, request)
        }
        assertThat(exception.errorCode).isEqualTo(ErrorCode.PAYMENT_ALREADY_COMPLETED)
    }
}