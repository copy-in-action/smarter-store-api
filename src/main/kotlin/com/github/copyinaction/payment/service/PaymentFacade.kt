package com.github.copyinaction.payment.service

import com.github.copyinaction.booking.repository.BookingRepository
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.coupon.service.CouponService
import com.github.copyinaction.payment.domain.PaymentCancelledEvent
import com.github.copyinaction.payment.domain.PaymentCompletedEvent
import com.github.copyinaction.payment.dto.PaymentCompleteRequest
import com.github.copyinaction.payment.dto.PaymentCreateRequest
import com.github.copyinaction.payment.dto.PaymentResponse
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * 결제 서비스의 복잡한 오케스트레이션을 담당하는 Facade 클래스입니다.
 * PaymentService(결제 도메인), CouponService(쿠폰 도메인), BookingService(예매 도메인) 간의 상호작용을 조율합니다.
 */
@Component
class PaymentFacade(
    private val paymentService: PaymentService,
    private val couponService: CouponService,
    private val bookingRepository: BookingRepository,
    private val eventPublisher: ApplicationEventPublisher
) {

    @Transactional
    fun createPayment(userId: Long, request: PaymentCreateRequest): PaymentResponse {
        val booking = bookingRepository.findByIdOrNull(request.bookingId)
            ?: throw CustomException(ErrorCode.BOOKING_NOT_FOUND)

        // 1. 사전 검증 및 엔티티 생성/조회 (PaymentService 위임)
        val payment = paymentService.preparePayment(userId, request, booking)

        // 2. 할인 내역 적용 및 쿠폰 사용 처리 (CouponService 연동 포함)
        paymentService.processDiscountsWithCoupons(userId, payment, booking, request)

        // 3. 최종 검증 및 저장
        payment.validateAmount(request.totalAmount)
        val savedPayment = paymentService.savePayment(payment)
        
        return PaymentResponse.from(savedPayment)
    }

    @Transactional
    fun completePayment(paymentId: UUID, request: PaymentCompleteRequest): PaymentResponse {
        // 1. 결제 완료 처리 (PaymentService 위임)
        val payment = paymentService.completePaymentInternal(paymentId, request)
        
        val response = PaymentResponse.from(payment)

        // 2. 외부 이벤트 발행 (결제 완료 후 후속 조치: 통계, 알림 등)
        eventPublisher.publishEvent(
            PaymentCompletedEvent(
                paymentId = payment.id,
                bookingId = payment.booking.id!!,
                userId = payment.userId,
                finalPrice = payment.finalPrice,
                discountAmount = payment.discountAmount,
                completedAt = payment.completedAt!!
            )
        )

        return response
    }

    @Transactional
    fun cancelPayment(bookingId: UUID, reason: String): PaymentResponse {
        // 1. 결제 취소 처리 (PaymentService 위임)
        val payment = paymentService.cancelPaymentInternal(bookingId, reason)

        // 2. 쿠폰 복구 처리 (도메인 간 조율)
        couponService.restoreCoupons(payment.id)

        // 3. 외부 이벤트 발행
        eventPublisher.publishEvent(
            PaymentCancelledEvent(
                paymentId = payment.id,
                bookingId = payment.booking.id!!,
                userId = payment.userId,
                cancelReason = payment.cancelReason!!,
                cancelledAt = payment.cancelledAt!!
            )
        )

        return PaymentResponse.from(payment)
    }
}
