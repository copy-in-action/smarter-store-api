package com.github.copyinaction.payment.service

import com.github.copyinaction.payment.domain.PaymentCancelledEvent
import com.github.copyinaction.payment.domain.PaymentCompletedEvent
import com.github.copyinaction.payment.domain.PaymentRefundedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PaymentEventHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePaymentCompleted(event: PaymentCompletedEvent) {
        log.info("결제 완료 이벤트 수신: paymentId={}, bookingId={}, amount={}", 
            event.paymentId, event.bookingId, event.finalPrice)
        // TODO: 매출 통계 업데이트 로직 추가 (Phase 3)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePaymentCancelled(event: PaymentCancelledEvent) {
        log.info("결제 취소 이벤트 수신: paymentId={}, reason={}", 
            event.paymentId, event.cancelReason)
        // TODO: 매출 통계 반영 및 예매 상태 변경 확인 (Phase 3)
    }

    @EventListener
    fun handlePaymentRefunded(event: PaymentRefundedEvent) {
        log.info("환불 완료 이벤트 수신: paymentId={}, amount={}", 
            event.paymentId, event.refundAmount)
    }
}
