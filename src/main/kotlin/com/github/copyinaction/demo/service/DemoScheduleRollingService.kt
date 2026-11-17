package com.github.copyinaction.demo.service

import com.github.copyinaction.demo.config.ConditionalOnDemoSeed
import com.github.copyinaction.demo.config.DemoSeedProperties
import com.github.copyinaction.performance.domain.PerformanceSchedule
import com.github.copyinaction.performance.domain.TicketOptionCommand
import com.github.copyinaction.performance.repository.PerformanceRepository
import com.github.copyinaction.performance.repository.PerformanceScheduleRepository
import com.github.copyinaction.venue.domain.SeatGrade
import com.github.copyinaction.venue.util.SeatingChartParser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * [Track 1-1] 노출 중인 공연의 회차를 오늘 기준 N일(기본 56일) 뒤까지 롤링 생성
 *
 * - 공연 단위로 트랜잭션을 분리(REQUIRES_NEW)해서, 한 공연의 오류가 전체를 롤백하거나 락을 오래 잡지 않게 한다.
 * - 기존 회차가 있으면 시간대/가격을 승계하고, 없으면 기본값(14:00, 19:30 / 등급별 기본가)을 사용한다.
 */
@Service
@ConditionalOnDemoSeed
class DemoScheduleRollingService(
    private val performanceRepository: PerformanceRepository,
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val seatingChartParser: SeatingChartParser,
    private val properties: DemoSeedProperties,
    transactionManager: PlatformTransactionManager
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val txTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    companion object {
        val DEFAULT_SHOW_TIMES: List<LocalTime> = listOf(LocalTime.of(14, 0), LocalTime.of(19, 30))

        val DEFAULT_PRICES: Map<SeatGrade, Int> = mapOf(
            SeatGrade.VIP to 150_000,
            SeatGrade.R to 120_000,
            SeatGrade.S to 90_000,
            SeatGrade.A to 70_000,
            SeatGrade.B to 50_000
        )

        /** 같은 시간대에 모든 공연의 종료일이 같지 않도록 공연 ID로 +0~14일 분산 */
        fun targetEndDate(today: LocalDate, rollingDays: Int, performanceId: Long): LocalDate =
            today.plusDays(rollingDays + (performanceId % 15))
    }

    /**
     * 노출 중(visible)이면서 공연장이 지정된 모든 공연의 회차를 롤링한다.
     *
     * @return 공연 ID -> 새로 생성된 회차 ID 목록 (좌석 시딩 단계로 전달)
     */
    fun rollAll(): Map<Long, List<Long>> {
        val visiblePerformances = performanceRepository.findAllByVisible(true)
        val (withVenue, withoutVenue) = visiblePerformances.partition { it.venue != null }
        if (withoutVenue.isNotEmpty()) {
            log.warn("[DemoSeed] 공연장이 없어 회차 롤링을 건너뛴 공연 {}건: {}", withoutVenue.size, withoutVenue.map { it.id })
        }

        val result = linkedMapOf<Long, List<Long>>()
        for (performance in withVenue) {
            val performanceId = performance.id
            try {
                val newScheduleIds = txTemplate.execute { rollPerformance(performanceId) } ?: emptyList()
                result[performanceId] = newScheduleIds
            } catch (e: Exception) {
                log.warn("[DemoSeed] 공연 {} 회차 롤링 실패: {}", performanceId, e.message, e)
            }
        }
        log.info(
            "[DemoSeed] 회차 롤링 완료: 공연 {}건, 신규 회차 {}건",
            result.size, result.values.sumOf { it.size }
        )
        return result
    }

    /**
     * 공연 1건의 회차 롤링 (호출자가 트랜잭션 경계를 제공해야 한다)
     *
     * @return 새로 생성된 회차 ID 목록 (엔티티는 트랜잭션 종료 후 분리되므로 ID만 반환)
     */
    fun rollPerformance(performanceId: Long): List<Long> {
        val performance = performanceRepository.findById(performanceId).orElse(null) ?: return emptyList()
        val venue = performance.venue ?: return emptyList()

        val seatsByGrade = seatingChartParser.countSeatsByGrade(venue.seatingChart).filterValues { it > 0 }
        if (seatsByGrade.isEmpty()) {
            log.warn("[DemoSeed] 공연 {} 의 공연장 {} 좌석배치도가 유효하지 않아 회차 롤링을 건너뜁니다.", performanceId, venue.id)
            return emptyList()
        }

        val today = LocalDate.now()
        val now = LocalDateTime.now()
        val rollingDays = properties.rollingDays

        // 기간 연장 (JPA dirty checking)
        val targetEnd = targetEndDate(today, rollingDays, performanceId)
        performance.extendEndDate(targetEnd)

        // 회차 템플릿 승계: 최근 회차의 시간대(최대 2개)와 등급별 가격
        val existingSchedules = performanceScheduleRepository.findByPerformanceId(performanceId)
        val existingShowTimes = existingSchedules.map { it.showDateTime }.toHashSet()
        val recentSchedules = existingSchedules.sortedByDescending { it.showDateTime }

        val showTimes = recentSchedules
            .map { it.showDateTime.toLocalTime() }
            .distinct()
            .take(2)
            .sorted()
            .ifEmpty { DEFAULT_SHOW_TIMES }

        val inheritedPrices: Map<SeatGrade, Int> = recentSchedules.firstOrNull()
            ?.ticketOptions
            ?.associate { it.seatGrade to it.price }
            ?: emptyMap()

        // 현재 공연장의 유효 등급과 매칭하고, 가격이 없는 등급은 기본가로 보완
        val ticketCommands = seatsByGrade.entries
            .sortedBy { it.key.ordinal }
            .map { (grade, quantity) ->
                TicketOptionCommand(
                    seatGrade = grade,
                    price = inheritedPrices[grade] ?: DEFAULT_PRICES.getValue(grade),
                    totalQuantity = quantity
                )
            }

        val newSchedules = mutableListOf<PerformanceSchedule>()
        for (offset in 1..rollingDays) {
            val date = today.plusDays(offset.toLong())
            for (time in showTimes) {
                val showDateTime = PerformanceSchedule.truncateToMinute(date.atTime(time))
                // 중복 검사: 이미 같은 시각의 회차가 있으면 생성하지 않는다. (미리 조회한 집합으로 N번의 exists 쿼리 회피)
                if (showDateTime in existingShowTimes) continue

                val schedule = PerformanceSchedule.create(
                    performance = performance,
                    showDateTime = showDateTime,
                    // 즉시 예매 가능하도록 판매 시작을 어제로 설정 (validate는 saleStart <= showDateTime 만 검증)
                    saleStartDateTime = now.minusDays(1)
                )
                schedule.addTicketOptions(ticketCommands)
                newSchedules.add(schedule)
            }
        }

        val saved = performanceScheduleRepository.saveAll(newSchedules)
        if (saved.isNotEmpty()) {
            log.info("[DemoSeed] 공연 {} 회차 {}건 생성 (종료일: {})", performanceId, saved.size, performance.endDate)
        }
        return saved.map { it.id }
    }
}
