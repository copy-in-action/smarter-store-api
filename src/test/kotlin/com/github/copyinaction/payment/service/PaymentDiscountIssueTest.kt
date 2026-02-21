package com.github.copyinaction.payment.service

import com.github.copyinaction.booking.domain.Booking
import com.github.copyinaction.booking.domain.BookingSeat
import com.github.copyinaction.booking.repository.BookingRepository
import com.github.copyinaction.coupon.domain.Coupon
import com.github.copyinaction.coupon.repository.CouponRepository
import com.github.copyinaction.coupon.repository.CouponUsageRepository
import com.github.copyinaction.coupon.service.CouponService
import com.github.copyinaction.discount.domain.DiscountType
import com.github.copyinaction.payment.domain.PaymentMethod
import com.github.copyinaction.payment.dto.PaymentDiscountRequest
import com.github.copyinaction.payment.dto.PaymentCreateRequest
import com.github.copyinaction.payment.repository.PaymentRepository
import com.github.copyinaction.venue.domain.SeatGrade
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime
import java.util.*

class PaymentDiscountIssueTest {
    private val paymentRepository = mockk<PaymentRepository>()
    private val bookingRepository = mockk<BookingRepository>()
    private val couponRepository = mockk<CouponRepository>()
    private val couponUsageRepository = mockk<CouponUsageRepository>()
    
    // Use real CouponService
    private val couponService = CouponService(couponRepository, couponUsageRepository)
    
    private val salesStatsService = mockk<com.github.copyinaction.stats.service.SalesStatsService>(relaxed = true)
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    
    // Use real PaymentService
    private val paymentService = PaymentService(
        paymentRepository, 
        bookingRepository, 
        couponService, 
        salesStatsService,
        eventPublisher
    )

    @Test
    @DisplayName("정상적인 데이터(5만원, 20프로 할인)로 결제 생성 시 올바르게 계산되어야 한다")
    fun reproducePaymentAmountIssue() {
        val bookingId = UUID.randomUUID()
        val userId = 1L
        val couponId = 5L
        val seatId = 701L
        
        // 1. Mock Data Setup
        val siteUser = mockk<com.github.copyinaction.auth.domain.User> {
            every { id } returns userId
        }
        
        // Mock Booking & Seat
        val booking = mockk<Booking>()
        val bookingSeat = mockk<BookingSeat>()
        val schedule = mockk<com.github.copyinaction.performance.domain.PerformanceSchedule>(relaxed = true)

        every { booking.id } returns bookingId
        every { booking.siteUser } returns siteUser
        every { booking.schedule } returns schedule
        every { booking.bookingSeats } returns mutableListOf(bookingSeat)
        
        // Seat Setup: Price 50,000
        every { bookingSeat.id } returns seatId
        every { bookingSeat.price } returns 50000
        every { bookingSeat.grade } returns SeatGrade.R 
        // needed for paymentItem
        every { bookingSeat.section } returns "A"
        every { bookingSeat.row } returns 1
        every { bookingSeat.col } returns 1
        
        // Coupon Setup: 20% Discount
        val coupon = Coupon.create(
            name = "1월 20프로 할인",
            discountRate = 20,
            validFrom = LocalDateTime.now().minusDays(1),
            validUntil = LocalDateTime.now().plusDays(1)
        )
        // Reflection set ID or just mock findById
        every { couponRepository.findById(couponId) } returns java.util.Optional.of(coupon)

        // 2. Request Setup
        val originalPrice = 50000
        val bookingFee = 2000
        // Expected Discount: 50000 * 0.2 = 10000
        val expectedDiscount = 10000
        val totalAmount = 42000 // 50000 + 2000 - 10000

        val discountDto = PaymentDiscountRequest(
            type = DiscountType.COUPON,
            name = "1월 20프로 할인",
            amount = expectedDiscount, // Client calculates 10000
            couponId = couponId,
            bookingSeatId = seatId
        )
        
        val request = PaymentCreateRequest(
            bookingId = bookingId,
            paymentMethod = PaymentMethod.VIRTUAL_ACCOUNT,
            totalAmount = totalAmount,
            ticketAmount = 40000,
            bookingFee = bookingFee,
            originalPrice = originalPrice,
            isAgreed = true,
            discounts = listOf(discountDto)
        )

        every { bookingRepository.findByIdOrNull(bookingId) } returns booking
        every { paymentRepository.findByBookingId(bookingId) } returns null
        every { paymentRepository.save(any()) } answers { firstArg() }
        every { couponUsageRepository.save(any()) } answers { firstArg() }

        // 3. Act
        val response = paymentService.createPayment(userId, request)

        // 4. Assert
        assertThat(response.finalPrice).isEqualTo(totalAmount)
    }
}
