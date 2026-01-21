package com.github.copyinaction.common.scheduler

import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.common.service.SlackService
import com.github.copyinaction.payment.domain.PaymentStatus
import com.github.copyinaction.payment.repository.PaymentRepository
import com.github.copyinaction.stats.service.SalesStatsService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime

@Component
class DailyStatsAggregationScheduler(
    private val paymentRepository: PaymentRepository,
    private val salesStatsService: SalesStatsService,
    private val slackService: SlackService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 매일 자정에 전일 통계 집계
     * - 실시간 이벤트 처리에서 누락된 데이터가 있을 수 있으므로 전일 데이터 재집계
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    fun aggregateYesterdayStats() {
        val yesterday = LocalDate.now().minusDays(1)
        log.info("일별 통계 집계 시작: {}", yesterday)

        try {
            val startDateTime = yesterday.atStartOfDay()
            val endDateTime = yesterday.atTime(LocalTime.MAX)

            val completedPayments = paymentRepository.findAllByStatusAndCompletedAtBetween(
                PaymentStatus.COMPLETED,
                startDateTime,
                endDateTime
            )

            if (completedPayments.isEmpty()) {
                log.info("집계 대상 결제 건 없음: {}", yesterday)
                slackService.sendDailyStatsSkipped(yesterday)
                return
            }

            salesStatsService.recalculateDailyStats(yesterday, completedPayments)
            log.info("일별 통계 집계 완료: date={}, paymentCount={}", yesterday, completedPayments.size)
            slackService.sendDailyStatsSuccess(yesterday, completedPayments.size)
        } catch (e: Exception) {
            val errorCode = ErrorCode.STATS_DAILY_AGGREGATION_FAILED
            log.error("[{}] {}: date={}", errorCode.name, errorCode.message, yesterday, e)
            slackService.sendDailyStatsFailure(yesterday, e.message ?: "알 수 없는 오류")
        }
    }
}
