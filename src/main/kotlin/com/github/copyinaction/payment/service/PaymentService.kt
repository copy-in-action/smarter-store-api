package com.github.copyinaction.payment.service

import com.github.copyinaction.booking.repository.BookingRepository
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.coupon.dto.SeatCouponRequest
import com.github.copyinaction.coupon.service.CouponService
import com.github.copyinaction.discount.domain.DiscountType
import com.github.copyinaction.discount.domain.PaymentDiscount
import com.github.copyinaction.payment.domain.Payment
import com.github.copyinaction.payment.domain.PaymentCancelledEvent
import com.github.copyinaction.payment.domain.PaymentCompletedEvent
import com.github.copyinaction.payment.domain.PaymentItem
import com.github.copyinaction.payment.dto.*
import com.github.copyinaction.payment.repository.PaymentRepository
import com.github.copyinaction.stats.service.SalesStatsService
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
    private val salesStatsService: SalesStatsService,
    private val eventPublisher: ApplicationEventPublisher
) {

    @Transactional
    fun createPayment(userId: Long, request: PaymentCreateRequest): PaymentResponse {
        val booking = bookingRepository.findByIdOrNull(request.bookingId)
            ?: throw CustomException(ErrorCode.BOOKING_NOT_FOUND)

        // 0. 사전 검증 (권한 및 약관 동의)
        if (booking.siteUser.id != userId) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }
        if (!request.isAgreed) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE)
        }

        // 1. Payment 엔티티 생성
        val payment = Payment.create(
            booking = booking,
            userId = userId,
            paymentMethod = request.paymentMethod,
            originalPrice = request.originalPrice,
            bookingFee = request.bookingFee
        )

        // 2. PaymentItem 상세 생성 (BookingSeat 기반 스냅샷)
        booking.bookingSeats.forEach { seat ->
            val paymentItem = PaymentItem.from(
                payment = payment,
                bookingSeat = seat,
                performance = booking.schedule.performance,
                schedule = booking.schedule
            )
            
            // 좌석별 할인이 적용된 경우 상세 내역 업데이트
            val seatDiscount = request.discounts.find { it.bookingSeatId == seat.id }
            if (seatDiscount != null) {
                paymentItem.applyDiscount(seatDiscount.amount)
            }
            
            payment.addPaymentItem(paymentItem)
        }

        // 3. 할인 내역 적용 및 저장 (쿠폰 사용 처리 포함)
        val couponDiscounts = mutableListOf<SeatCouponRequest>()
        
        request.discounts.forEach { discountDto ->
            val discount = PaymentDiscount.create(
                payment = payment,
                type = discountDto.type,
                name = discountDto.name,
                amount = discountDto.amount,
                referenceId = discountDto.referenceId
            )
            payment.addDiscount(discount)

            // 쿠폰인 경우 수집 (나중에 한꺼번에 처리)
            if (discountDto.type == DiscountType.COUPON && discountDto.referenceId != null) {
                couponDiscounts.add(SeatCouponRequest(
                    seatId = discountDto.bookingSeatId ?: 0L, // 좌석 ID (없으면 0)
                    couponId = discountDto.referenceId.toLong(), // referenceId를 쿠폰 ID로 사용
                    originalPrice = request.originalPrice // 원가 정보
                ))
            }
        }
        
        // 수집된 쿠폰이 있으면 한꺼번에 사용 처리
        if (couponDiscounts.isNotEmpty()) {
            couponService.useCoupons(userId, payment.id, couponDiscounts)
        }

        // 4. 금액 검증 (서버 계산 vs 클라이언트 요청)
        payment.validateAmount(request.totalAmount)

        val savedPayment = paymentRepository.save(payment)
        
        return PaymentResponse.from(savedPayment)
    }

    @Transactional
    fun completePayment(paymentId: UUID, request: PaymentCompleteRequest): PaymentResponse {
        val payment = paymentRepository.findByIdOrNull(paymentId)
            ?: throw CustomException(ErrorCode.PAYMENT_NOT_FOUND)

        payment.complete(request.pgProvider, request.pgTransactionId)
        
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

        // 쿠폰 복구 처리 (결제 ID 기준 전체 복구)
        couponService.restoreCoupons(payment.id)

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