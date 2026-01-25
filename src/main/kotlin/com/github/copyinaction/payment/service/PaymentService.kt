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

        // 0-1. 원가 검증 (클라이언트 요청 vs 서버 좌석 가격)
        val actualOriginalPrice = booking.bookingSeats.sumOf { it.price }
        if (actualOriginalPrice != request.originalPrice) {
            val seatDetails = booking.bookingSeats.joinToString(", ") {
                "[좌석ID: ${it.id}] ${it.section}구역 ${it.row}행 ${it.col}열: ${it.price}원"
            }
            throw CustomException(
                ErrorCode.INVALID_INPUT_VALUE,
                "결제 요청 원가가 서버에서 계산된 값과 일치하지 않습니다. (예매ID: ${request.bookingId}, 요청: ${request.originalPrice}원, 서버 합계: $actualOriginalPrice원). 상세 내역: $seatDetails"
            )
        }

        // 1. Payment 엔티티 조회 또는 생성 (재결제 지원)
        val existingPayment = paymentRepository.findByBookingId(request.bookingId)
        val payment = if (existingPayment != null) {
            // 기존 결제 시도가 있는 경우: 쿠폰 복구 및 상태 초기화 후 재사용
            // (PENDING/FAILED 상태인 경우만 update 메서드 내에서 허용됨)
            couponService.restoreCoupons(existingPayment.id)
            existingPayment.update(request.paymentMethod)
            existingPayment
        } else {
            // 신규 결제 생성
            Payment.create(
                booking = booking,
                userId = userId,
                paymentMethod = request.paymentMethod,
                originalPrice = request.originalPrice,
                bookingFee = request.bookingFee
            )
        }

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
            var discountAmount = discountDto.amount

            // 쿠폰인 경우 서버에서 금액 재계산 (클라이언트 값 무시)
            if (discountDto.type == DiscountType.COUPON && discountDto.couponId != null) {
                val targetPrice = if (discountDto.bookingSeatId != null) {
                    booking.bookingSeats.find { it.id == discountDto.bookingSeatId }?.price
                        ?: throw CustomException(
                            ErrorCode.INVALID_INPUT_VALUE,
                            "예매에 해당 좌석이 존재하지 않습니다. (요청된 좌석ID: ${discountDto.bookingSeatId}, 현재 예매 좌석ID 목록: ${booking.bookingSeats.map { it.id }})"
                        )
                } else {
                    // 좌석 지정 없는 쿠폰은 일단 원가 기준 (정책에 따라 다를 수 있음)
                    request.originalPrice
                }
                discountAmount = couponService.calculateDiscount(discountDto.couponId, targetPrice)
            }

            val discount = PaymentDiscount.create(
                payment = payment,
                type = discountDto.type,
                name = discountDto.name,
                amount = discountAmount,
                couponId = discountDto.couponId
            )
            payment.addDiscount(discount)

            // 쿠폰인 경우 수집 (나중에 한꺼번에 처리)
            if (discountDto.type == DiscountType.COUPON && discountDto.couponId != null) {
                couponDiscounts.add(SeatCouponRequest(
                    bookingSeatId = discountDto.bookingSeatId ?: 0L, // 좌석 ID (없으면 0)
                    couponId = discountDto.couponId, 
                    originalPrice = request.originalPrice // 여기도 개별 좌석 가격이 맞지만, CouponService 내부에서 재계산하므로 무관
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
                seatGrade = it.seatGrade,
                section = it.section,
                row = it.rowNum,
                col = it.colNum,
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