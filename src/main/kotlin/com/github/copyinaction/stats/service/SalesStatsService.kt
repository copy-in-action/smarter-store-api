package com.github.copyinaction.stats.service

import com.github.copyinaction.discount.domain.DiscountType
import com.github.copyinaction.payment.domain.Payment
import com.github.copyinaction.payment.domain.PaymentMethod
import com.github.copyinaction.stats.domain.DailySalesStats
import com.github.copyinaction.stats.domain.DiscountStats
import com.github.copyinaction.stats.domain.PaymentMethodStats
import com.github.copyinaction.stats.domain.PerformanceSalesStats
import com.github.copyinaction.stats.repository.DailySalesStatsRepository
import com.github.copyinaction.stats.repository.DiscountStatsRepository
import com.github.copyinaction.stats.repository.PaymentMethodStatsRepository
import com.github.copyinaction.stats.repository.PerformanceSalesStatsRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class SalesStatsService(
    private val dailySalesStatsRepository: DailySalesStatsRepository,
    private val performanceSalesStatsRepository: PerformanceSalesStatsRepository,
    private val paymentMethodStatsRepository: PaymentMethodStatsRepository,
    private val discountStatsRepository: DiscountStatsRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun updateAllStats(payment: Payment) {
        val date = payment.completedAt?.toLocalDate() ?: LocalDate.now()
        val amount = payment.finalPrice.toLong()
        val ticketCount = payment.paymentItems.size
        val discountTotal = payment.discountAmount.toLong()

        // 1. 일별 매출
        updateDailySales(date, amount, ticketCount, discountTotal)

        // 2. 공연별 매출 (첫 번째 아이템 기준 공연 ID 추출)
        payment.paymentItems.firstOrNull()?.let {
            updatePerformanceSales(it.performanceId, date, amount, ticketCount, discountTotal)
        }

        // 3. 결제 수단별
        updatePaymentMethodStats(payment.paymentMethod, date, amount)

        // 4. 할인 종류별
        payment.discounts.forEach {
            updateDiscountStats(it.discountType, date, it.discountAmount.toLong())
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun updateDailySales(date: LocalDate, amount: Long, ticketCount: Int, discountAmount: Long) {
        val stats = dailySalesStatsRepository.findByDateWithLock(date)
            .orElseGet { dailySalesStatsRepository.save(DailySalesStats(date = date)) }
        stats.incrementSales(amount, ticketCount, discountAmount)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun updatePerformanceSales(performanceId: Long, date: LocalDate, amount: Long, ticketCount: Int, discountAmount: Long) {
        val stats = performanceSalesStatsRepository.findByPerformanceIdAndDateWithLock(performanceId, date)
            .orElseGet { performanceSalesStatsRepository.save(PerformanceSalesStats(performanceId = performanceId, date = date)) }
        // TODO: 실제 공연의 총 좌석수를 가져오는 로직 연동 필요 (현재는 0 또는 기본값)
        stats.update(amount, ticketCount, discountAmount, 0)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun updatePaymentMethodStats(method: PaymentMethod, date: LocalDate, amount: Long) {
        val stats = paymentMethodStatsRepository.findByPaymentMethodAndDateWithLock(method, date)
            .orElseGet { paymentMethodStatsRepository.save(PaymentMethodStats(paymentMethod = method, date = date)) }
        stats.increment(amount)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun updateDiscountStats(type: DiscountType, date: LocalDate, amount: Long) {
        val stats = discountStatsRepository.findByDiscountTypeAndDateWithLock(type, date)
            .orElseGet { discountStatsRepository.save(DiscountStats(discountType = type, date = date)) }
        stats.increment(amount)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordDailyRefund(date: LocalDate, amount: Long) {
        val stats = dailySalesStatsRepository.findByDateWithLock(date)
            .orElseGet {
                dailySalesStatsRepository.save(DailySalesStats(date = date))
            }

        stats.recordRefund(amount)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordDailyCancel(date: LocalDate) {
        val stats = dailySalesStatsRepository.findByDateWithLock(date)
            .orElseGet {
                dailySalesStatsRepository.save(DailySalesStats(date = date))
            }

        stats.recordCancel()
    }

    @Transactional(readOnly = true)
    fun getDailySales(date: LocalDate): DailySalesStats {
        return dailySalesStatsRepository.findById(date)
            .orElseThrow { com.github.copyinaction.common.exception.CustomException(com.github.copyinaction.common.exception.ErrorCode.RESOURCE_NOT_FOUND) }
    }

    @Transactional(readOnly = true)
    fun getPerformanceSales(performanceId: Long): List<PerformanceSalesStats> {
        return performanceSalesStatsRepository.findAllByPerformanceIdOrderByDateDesc(performanceId)
    }

    @Transactional(readOnly = true)
    fun getPaymentMethodStats(date: LocalDate): List<PaymentMethodStats> {
        return paymentMethodStatsRepository.findAllByDate(date)
    }

    @Transactional(readOnly = true)
    fun getDiscountStats(date: LocalDate): List<DiscountStats> {
        return discountStatsRepository.findAllByDate(date)
    }

    @Transactional
    fun recalculateDailyStats(date: LocalDate, payments: List<Payment>) {
        // 기존 통계 삭제
        dailySalesStatsRepository.findById(date).ifPresent { dailySalesStatsRepository.delete(it) }
        performanceSalesStatsRepository.findAllByDate(date).forEach { performanceSalesStatsRepository.delete(it) }
        paymentMethodStatsRepository.findAllByDate(date).forEach { paymentMethodStatsRepository.delete(it) }
        discountStatsRepository.findAllByDate(date).forEach { discountStatsRepository.delete(it) }

        // 새로 집계
        payments.forEach { payment ->
            try {
                updateAllStats(payment)
            } catch (e: Exception) {
                log.error("통계 재집계 중 오류 발생 - date: {}, paymentId: {}, paymentNumber: {}", 
                    date, payment.id, payment.paymentNumber, e)
                throw e
            }
        }
    }
}
