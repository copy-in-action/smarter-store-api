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
    private val couponService: CouponService
) {

    /**
     * 결제 준비 (검증 및 엔티티 생성/수정)
     */
    @Transactional
    fun preparePayment(userId: Long, request: PaymentCreateRequest, booking: com.github.copyinaction.booking.domain.Booking): Payment {
        // 1. 사전 검증
        validatePaymentRequest(userId, request, booking)

        // 2. Payment 엔티티 조회 또는 생성 (재결제 지원)
        val payment = getOrCreatePayment(userId, request, booking)

        // 3. PaymentItem 상세 생성 (스냅샷)
        createPaymentItems(payment, booking, request.discounts)

        return payment
    }

    /**
     * 할인 및 쿠폰 적용 처리
     */
    @Transactional
    fun processDiscountsWithCoupons(userId: Long, payment: Payment, booking: com.github.copyinaction.booking.domain.Booking, request: PaymentCreateRequest) {
        val couponDiscounts = mutableListOf<SeatCouponRequest>()
        
        request.discounts.forEach { discountDto ->
            val discountAmount = calculateFinalDiscountAmount(discountDto, booking, request.originalPrice)
            
            val discount = PaymentDiscount.create(
                payment = payment,
                type = discountDto.type,
                name = discountDto.name,
                amount = discountAmount,
                couponId = discountDto.couponId,
                bookingSeatId = discountDto.bookingSeatId
            )
            payment.addDiscount(discount)

            if (discountDto.type == DiscountType.COUPON && discountDto.couponId != null) {
                couponDiscounts.add(SeatCouponRequest(
                    bookingSeatId = discountDto.bookingSeatId ?: 0L,
                    couponId = discountDto.couponId, 
                    originalPrice = request.originalPrice
                ))
            }
        }
        
        if (couponDiscounts.isNotEmpty()) {
            couponService.useCoupons(userId, payment.id, couponDiscounts)
        }
    }

    /**
     * 최종 저장 및 반환
     */
    @Transactional
    fun savePayment(payment: Payment): Payment {
        return paymentRepository.save(payment)
    }

    private fun validatePaymentRequest(userId: Long, request: PaymentCreateRequest, booking: com.github.copyinaction.booking.domain.Booking) {
        if (booking.siteUser.id != userId) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }
        if (!request.isAgreed) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE)
        }

        val actualOriginalPrice = booking.bookingSeats.sumOf { it.price }
        if (actualOriginalPrice != request.originalPrice) {
            val seatDetails = booking.bookingSeats.joinToString(", ") {
                "[좌석ID: ${it.id}] ${it.section}구역 ${it.row}행 ${it.col}열: ${it.price}원"
            }
            throw CustomException(
                ErrorCode.PAYMENT_AMOUNT_MISMATCH,
                "결제 요청 원가가 서버 데이터와 일치하지 않습니다. (요청: ${request.originalPrice}, 서버: $actualOriginalPrice). 상세: $seatDetails"
            )
        }
    }

    private fun getOrCreatePayment(userId: Long, request: PaymentCreateRequest, booking: com.github.copyinaction.booking.domain.Booking): Payment {
        val existingPayment = paymentRepository.findByBookingId(request.bookingId)
        return if (existingPayment != null) {
            // 기존 결제 시도 시 사용했던 쿠폰 복구 (신규 적용을 위해 초기화)
            couponService.restoreCoupons(existingPayment.id)
            existingPayment.update(request.paymentMethod)
            existingPayment
        } else {
            Payment.create(
                booking = booking,
                userId = userId,
                paymentMethod = request.paymentMethod,
                originalPrice = request.originalPrice,
                bookingFee = request.bookingFee
            )
        }
    }

    private fun createPaymentItems(payment: Payment, booking: com.github.copyinaction.booking.domain.Booking, discounts: List<PaymentDiscountRequest>) {
        booking.bookingSeats.forEach { seat ->
            val paymentItem = PaymentItem.from(
                payment = payment,
                bookingSeat = seat,
                performance = booking.schedule.performance,
                schedule = booking.schedule
            )
            
            discounts.find { it.bookingSeatId == seat.id }?.let { 
                paymentItem.applyDiscount(it.amount) 
            }
            
            payment.addPaymentItem(paymentItem)
        }
    }

    private fun calculateFinalDiscountAmount(discountDto: PaymentDiscountRequest, booking: com.github.copyinaction.booking.domain.Booking, totalOriginalPrice: Int): Int {
        if (discountDto.type != DiscountType.COUPON || discountDto.couponId == null) {
            return discountDto.amount
        }

        val targetPrice = if (discountDto.bookingSeatId != null) {
            booking.bookingSeats.find { it.id == discountDto.bookingSeatId }?.price
                ?: throw CustomException(ErrorCode.BOOKING_SEAT_NOT_FOUND)
        } else {
            totalOriginalPrice
        }
        
        return couponService.calculateDiscount(discountDto.couponId, targetPrice)
    }

    @Transactional
    fun completePaymentInternal(paymentId: UUID, request: PaymentCompleteRequest): Payment {
        val payment = paymentRepository.findByIdOrNull(paymentId)
            ?: throw CustomException(ErrorCode.PAYMENT_NOT_FOUND)

        payment.complete(request.pgProvider, request.pgTransactionId)
        
        payment.cardCompany = request.cardCompany
        payment.cardNumberMasked = request.cardNumberMasked
        payment.installmentMonths = request.installmentMonths

        return payment
    }

    @Transactional
    fun cancelPaymentInternal(bookingId: UUID, reason: String): Payment {
        val payment = paymentRepository.findByBookingId(bookingId)
            ?: throw CustomException(ErrorCode.PAYMENT_NOT_FOUND)

        payment.cancel(reason)

        return payment
    }

    fun getPayment(paymentId: UUID, userId: Long): PaymentDetailResponse {
        val payment = paymentRepository.findByIdOrNull(paymentId)
            ?: throw CustomException(ErrorCode.PAYMENT_NOT_FOUND)

        if (payment.userId != userId) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        val items = payment.paymentItems.map {
            PaymentItemResponse(
                performanceId = it.performanceId,
                performanceTitle = it.performanceTitle,
                seatGrade = it.seatGrade,
                section = it.section,
                row = it.rowNum,
                col = it.colNum,
                unitPrice = it.unitPrice,
                discountAmount = it.discountAmount,
                finalPrice = it.finalPrice
            )
        }

        val discounts = payment.discounts.map {
            PaymentDiscountResponse.from(it)
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
            discounts = discounts,
            pgInfo = pgInfo
        )
    }

    fun getPaymentsByUser(userId: Long): List<PaymentResponse> {
        return paymentRepository.findAllByUserIdOrderByRequestedAtDesc(userId)
            .map { PaymentResponse.from(it) }
    }
}
