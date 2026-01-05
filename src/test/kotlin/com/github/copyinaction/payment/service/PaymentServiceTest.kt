package com.github.copyinaction.payment.service

import com.github.copyinaction.booking.domain.Booking
import com.github.copyinaction.booking.repository.BookingRepository
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.payment.domain.Payment
import com.github.copyinaction.payment.domain.PaymentMethod
import com.github.copyinaction.payment.domain.PaymentStatus
import com.github.copyinaction.payment.dto.PaymentCompleteRequest
import com.github.copyinaction.payment.dto.PaymentCreateRequest
import com.github.copyinaction.payment.repository.PaymentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.util.*

class PaymentServiceTest {
    private val paymentRepository = mockk<PaymentRepository>()
    private val bookingRepository = mockk<BookingRepository>()
    private val eventPublisher = mockk<org.springframework.context.ApplicationEventPublisher>(relaxed = true)
    private val paymentService = PaymentService(paymentRepository, bookingRepository, eventPublisher)

    @Test
    @DisplayName("새로운 결제를 생성할 수 있다")
    fun createPayment() {
        val bookingId = UUID.randomUUID()
        val userId = 1L
        val siteUser = mockk<com.github.copyinaction.auth.domain.User> {
            every { id } returns userId
        }
        val booking = mockk<Booking> {
            every { id } returns bookingId
            every { this@mockk.siteUser } returns siteUser
        }
        val request = PaymentCreateRequest(bookingId, PaymentMethod.CREDIT_CARD, 50000, 1000)

        every { bookingRepository.findByIdOrNull(bookingId) } returns booking
        every { paymentRepository.save(any()) } answers { firstArg() }

        val response = paymentService.createPayment(userId, request)

        assertThat(response.bookingId).isEqualTo(bookingId)
        assertThat(response.paymentStatus).isEqualTo(PaymentStatus.PENDING)
        verify { paymentRepository.save(any()) }
    }

    @Test
    @DisplayName("결제를 완료 처리할 수 있다")
    fun completePayment() {
        val paymentId = UUID.randomUUID()
        val payment = mockk<Payment>(relaxed = true) {
            every { id } returns paymentId
            every { paymentStatus } returns PaymentStatus.PENDING
        }
        val request = PaymentCompleteRequest("KAKAOPAY", "TID-12345")

        every { paymentRepository.findByIdOrNull(paymentId) } returns payment

        paymentService.completePayment(paymentId, request)

        verify { payment.complete("KAKAOPAY", "TID-12345") }
    }

    @Test
    @DisplayName("존재하지 않는 결제 완료 시도시 예외가 발생한다")
    fun completePaymentFail() {
        val paymentId = UUID.randomUUID()
        val request = PaymentCompleteRequest("KAKAOPAY", "TID-12345")

        every { paymentRepository.findByIdOrNull(paymentId) } returns null

        assertThatThrownBy { paymentService.completePayment(paymentId, request) }
            .isInstanceOf(CustomException::class.java)
    }
}
