package com.github.copyinaction.payment.service

import com.github.copyinaction.booking.domain.BookingSeat
import com.github.copyinaction.booking.repository.BookingRepository
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.coupon.service.CouponService
import com.github.copyinaction.discount.domain.DiscountType
import com.github.copyinaction.discount.domain.PaymentDiscount
import com.github.copyinaction.payment.domain.Payment
import com.github.copyinaction.payment.domain.PaymentCancelledEvent
import com.github.copyinaction.payment.domain.PaymentCompletedEvent
import com.github.copyinaction.payment.dto.*
import com.github.copyinaction.payment.repository.PaymentRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val bookingRepository: BookingRepository,
    private val couponService: CouponService,
    private val eventPublisher: ApplicationEventPublisher
) {

    @Transactional
    fun createPayment(userId: Long, request: PaymentCreateRequest): PaymentResponse {
        val booking = bookingRepository.findByIdOrNull(request.bookingId)
            ?: throw CustomException(ErrorCode.BOOKING_NOT_FOUND)

        // 사용자 권한 체크 (예매자 본인인지 확인)
        if (booking.siteUser.id != userId) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        // 1. Payment 엔티티 생성
        val payment = Payment.create(
            booking = booking,
            userId = userId,
            paymentMethod = request.paymentMethod,
            originalPrice = request.originalPrice,
            bookingFee = request.bookingFee
        )

        // 2. 할인 내역 적용 및 저장
        request.discounts.forEach { discountDto ->
            val discount = PaymentDiscount.create(
                payment = payment,
                type = discountDto.type,
                name = discountDto.name,
                amount = discountDto.amount,
                referenceId = discountDto.referenceId
            )
            payment.addDiscount(discount)

            // 쿠폰인 경우 사용 처리
            if (discountDto.type == DiscountType.COUPON && discountDto.referenceId != null) {
                couponService.useCoupon(userId, discountDto.referenceId, payment.id, request.originalPrice)
            }
        }

        // 3. 금액 검증 (서버 계산 vs 클라이언트 요청)
        payment.validateAmount(request.totalAmount)

        val savedPayment = paymentRepository.save(payment)
        
        // 이벤트 발행
        val response = PaymentResponse.from(savedPayment)
        return response
    }

    @Transactional
    fun completePayment(paymentId: UUID, request: PaymentCompleteRequest): PaymentResponse {
        val payment = paymentRepository.findByIdOrNull(paymentId)
            ?: throw CustomException(ErrorCode.PAYMENT_NOT_FOUND)

        payment.complete(request.pgProvider, request.pgTransactionId)
        
        // 추가 정보 업데이트
        payment.cardCompany = request.cardCompany
        payment.cardNumberMasked = request.cardNumberMasked
        payment.installmentMonths = request.installmentMonths

        val response = PaymentResponse.from(payment)

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
    fun cancelPayment(paymentId: UUID, userId: Long, request: PaymentCancelRequest): PaymentResponse {
        val payment = paymentRepository.findByIdOrNull(paymentId)
            ?: throw CustomException(ErrorCode.PAYMENT_NOT_FOUND)

        if (payment.userId != userId) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        payment.cancel(request.reason)

        // 쿠폰 복구 처리
        couponService.restoreCoupon(userId, payment.id)

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

    fun getPayment(paymentId: UUID, userId: Long): PaymentDetailResponse {
        val payment = paymentRepository.findByIdOrNull(paymentId)
            ?: throw CustomException(ErrorCode.PAYMENT_NOT_FOUND)

        if (payment.userId != userId) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        val items = payment.paymentItems.map {
            PaymentItemResponse(
                performanceTitle = it.performanceTitle,
                seatLabel = it.seatLabel,
                finalPrice = it.finalPrice
            )
        }

        val pgInfo = payment.pgProvider?.let {
            PgInfoResponse(
                provider = it,
                transactionId = payment.pgTransactionId ?: "",
                cardCompany = payment.cardCompany,
                cardNumber = payment.cardNumberMasked
            )
        }

        return PaymentDetailResponse(
            payment = PaymentResponse.from(payment),
            items = items,
            pgInfo = pgInfo
        )
    }

    fun getPaymentsByUser(userId: Long): List<PaymentResponse> {
        return paymentRepository.findAllByUserIdOrderByRequestedAtDesc(userId)
            .map { PaymentResponse.from(it) }
    }
}
