package com.github.copyinaction.payment.service

import com.github.copyinaction.payment.domain.PaymentCancelledEvent
import com.github.copyinaction.payment.domain.PaymentCompletedEvent
import com.github.copyinaction.payment.domain.PaymentRefundedEvent
import com.github.copyinaction.payment.repository.PaymentRepository
import com.github.copyinaction.stats.service.SalesStatsService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PaymentEventHandler(
    private val salesStatsService: SalesStatsService,
    private val paymentRepository: PaymentRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePaymentCompleted(event: PaymentCompletedEvent) {
        log.info("결제 완료 이벤트 수신: paymentId={}, bookingId={}, amount={}", 
            event.paymentId, event.bookingId, event.finalPrice)
        
        val payment = paymentRepository.findById(event.paymentId).orElse(null) ?: return
        
        salesStatsService.updateAllStats(payment)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePaymentCancelled(event: PaymentCancelledEvent) {
        log.info("결제 취소 이벤트 수신: paymentId={}, reason={}",
            event.paymentId, event.cancelReason)

        salesStatsService.recordDailyCancel(event.cancelledAt.toLocalDate())
    }

    @EventListener
    fun handlePaymentRefunded(event: PaymentRefundedEvent) {
        log.info("환불 완료 이벤트 수신: paymentId={}, amount={}", 
            event.paymentId, event.refundAmount)
            
        salesStatsService.recordDailyRefund(
            date = event.refundedAt.toLocalDate(),
            amount = event.refundAmount.toLong()
        )
    }
}
