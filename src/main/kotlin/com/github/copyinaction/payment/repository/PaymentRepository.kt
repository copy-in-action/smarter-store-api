package com.github.copyinaction.payment.repository

import com.github.copyinaction.payment.domain.Payment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PaymentRepository : JpaRepository<Payment, UUID> {
    fun findByPaymentNumber(paymentNumber: String): Payment?
    
    fun findAllByUserIdOrderByRequestedAtDesc(userId: Long): List<Payment>
    
    fun findByBookingId(bookingId: UUID): Payment?
}
