package com.github.copyinaction.payment.domain

import java.time.LocalDateTime
import java.util.*

/**
 * 결제 완료 이벤트
 */
data class PaymentCompletedEvent(
    val paymentId: UUID,
    val bookingId: UUID,
    val userId: Long,
    val finalPrice: Int,
    val discountAmount: Int,
    val completedAt: LocalDateTime
)

/**
 * 결제 취소 이벤트
 */
data class PaymentCancelledEvent(
    val paymentId: UUID,
    val bookingId: UUID,
    val userId: Long,
    val cancelReason: String,
    val cancelledAt: LocalDateTime
)

/**
 * 환불 완료 이벤트
 */
data class PaymentRefundedEvent(
    val paymentId: UUID,
    val bookingId: UUID,
    val userId: Long,
    val refundAmount: Int,
    val refundedAt: LocalDateTime
)
