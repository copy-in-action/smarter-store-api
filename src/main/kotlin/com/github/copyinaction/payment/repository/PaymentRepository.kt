package com.github.copyinaction.payment.repository

import com.github.copyinaction.payment.domain.Payment
import com.github.copyinaction.payment.domain.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface PaymentRepository : JpaRepository<Payment, UUID> {
    fun findByPaymentNumber(paymentNumber: String): Payment?

    fun findAllByUserIdOrderByRequestedAtDesc(userId: Long): List<Payment>

    fun findByBookingId(bookingId: UUID): Payment?

    fun findAllByBookingIdIn(bookingIds: Collection<UUID>): List<Payment>

    @Query("""
        SELECT p FROM Payment p
        LEFT JOIN FETCH p.paymentItems
        WHERE p.paymentStatus = :status
        AND p.completedAt >= :startDateTime
        AND p.completedAt < :endDateTime
    """)
    fun findAllByStatusAndCompletedAtBetween(
        status: PaymentStatus,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): List<Payment>
}
