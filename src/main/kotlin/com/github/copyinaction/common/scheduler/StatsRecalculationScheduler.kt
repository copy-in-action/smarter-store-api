package com.github.copyinaction.common.scheduler

import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.payment.domain.PaymentStatus
import com.github.copyinaction.payment.repository.PaymentRepository
import com.github.copyinaction.stats.service.SalesStatsService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalTime

@Component
class StatsRecalculationScheduler(
    private val paymentRepository: PaymentRepository,
    private val salesStatsService: SalesStatsService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 매주 일요일 새벽 3시에 최근 7일간 통계 재계산
     * - 데이터 정합성 보장을 위한 주간 재계산
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    fun recalculateWeeklyStats() {
        val today = LocalDate.now()
        val startDate = today.minusDays(7)
        log.info("주간 통계 재계산 시작: {} ~ {}", startDate, today.minusDays(1))

        try {
            var totalRecalculated = 0

            for (i in 7 downTo 1) {
                val targetDate = today.minusDays(i.toLong())
                val startDateTime = targetDate.atStartOfDay()
                val endDateTime = targetDate.atTime(LocalTime.MAX)

                val completedPayments = paymentRepository.findAllByStatusAndCompletedAtBetween(
                    PaymentStatus.COMPLETED,
                    startDateTime,
                    endDateTime
                )

                if (completedPayments.isNotEmpty()) {
                    salesStatsService.recalculateDailyStats(targetDate, completedPayments)
                    totalRecalculated += completedPayments.size
                    log.debug("통계 재계산: date={}, count={}", targetDate, completedPayments.size)
                }
            }

            log.info("주간 통계 재계산 완료: 총 {}건 처리", totalRecalculated)
        } catch (e: Exception) {
            val errorCode = ErrorCode.STATS_RECALCULATION_FAILED
            log.error("[{}] {}: period={} ~ {}", errorCode.name, errorCode.message, startDate, today.minusDays(1), e)
        }
    }
}
