package com.github.copyinaction.demo.service

import com.github.copyinaction.booking.service.BookingFacade
import com.github.copyinaction.demo.config.ConditionalOnDemoSeed
import com.github.copyinaction.demo.config.DemoSeedProperties
import com.github.copyinaction.seat.service.SseService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * [Track 2] 실시간 좌석 점유 시뮬레이터
 *
 * 누군가 좌석 배치도 화면(SSE)을 보고 있는 회차에서만 봇이 좌석을 점유/해제/확정하여
 * 데모 화면에서 노란색(점유) -> 해제/확정 전환을 실시간으로 볼 수 있게 한다.
 * 모든 동작은 실제 예매 플로우(`BookingFacade`)를 그대로 사용하므로 SSE 이벤트도 자연스럽게 전파된다.
 */
@Component
@ConditionalOnDemoSeed
class DemoSeatSimulator(
    private val sseService: SseService,
    private val bookingFacade: BookingFacade,
    private val userSeedProvider: DemoUserSeedProvider,
    private val seatSupport: DemoSeatSupport,
    private val properties: DemoSeedProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 봇이 점유 중인 PENDING 예매 추적 (인메모리) */
    private val holds = ConcurrentHashMap<UUID, SimulatedHold>()

    @Volatile
    private var cachedBotIds: List<Long> = emptyList()

    companion object {
        /** 회차당 동시에 유지할 봇 점유 건수 (이 값 미만이면 새로 점유) */
        const val TARGET_HOLDS_PER_SCHEDULE = 5

        /** 한 틱에 회차당 새로 추가하는 최대 점유 건수 (빈 화면이 서서히 채워지도록 제한) */
        const val MAX_NEW_HOLDS_PER_SCHEDULE_PER_TICK = 2

        /** 점유 후 확정/해제까지의 대기 시간 범위(초) */
        const val MIN_HOLD_SECONDS = 12L
        const val MAX_HOLD_SECONDS = 25L

        /** 이 점유율 미만일 때만 일부(5%)를 확정하고 나머지는 해제한다. */
        const val CONFIRM_OCCUPANCY_LIMIT = 0.80
        const val CONFIRM_PROBABILITY = 0.05

        /** 한 틱에 새로 점유를 시도하는 최대 회차 수 (부하 제한) */
        const val MAX_SCHEDULES_PER_TICK = 5
    }

    @Scheduled(fixedDelayString = "\${demo.seed.simulator-interval-ms:5000}")
    fun tick() {
        if (!properties.simulatorEnabled) return

        try {
            // 이미 점유 중인 건은 구독자가 없어도 정리해야 새 접속자에게 유령 점유가 보이지 않는다.
            settleDueHolds()

            val activeScheduleIds = sseService.getActiveScheduleIds()
            if (activeScheduleIds.isEmpty()) return

            startNewHolds(activeScheduleIds)
        } catch (e: Exception) {
            log.warn("[DemoSeed] 좌석 시뮬레이터 실행 중 오류: {}", e.message, e)
        }
    }

    /**
     * 25~50초가 지난 점유를 처리한다.
     * 점유율이 80% 미만이면 15%는 확정, 나머지는 해제한다.
     */
    private fun settleDueHolds() {
        val now = LocalDateTime.now()
        holds.values.filter { !now.isBefore(it.settleAt) }.forEach { hold ->
            holds.remove(hold.bookingId)
            try {
                val occupancy = seatSupport.occupancyRatio(hold.scheduleId)
                val confirm = occupancy < CONFIRM_OCCUPANCY_LIMIT && Random.nextDouble() < CONFIRM_PROBABILITY
                if (confirm) {
                    bookingFacade.confirmBooking(hold.bookingId, hold.userId)
                } else {
                    bookingFacade.cancelBooking(hold.bookingId, hold.userId, "데모 시뮬레이터 해제")
                }
            } catch (e: Exception) {
                // 만료 스케줄러가 먼저 정리했거나 이미 처리된 경우 - 시뮬레이터에서는 무시
                log.debug("[DemoSeed] 시뮬레이터 점유 정리 실패 (bookingId={}): {}", hold.bookingId, e.message)
            }
        }
    }

    /**
     * 구독자가 있는 회차 중 봇 점유가 부족한 회차에 새 점유를 만든다.
     */
    private fun startNewHolds(activeScheduleIds: Set<Long>) {
        val botIds = loadBotIds()
        if (botIds.isEmpty()) return

        val targets = activeScheduleIds.shuffled().take(MAX_SCHEDULES_PER_TICK)

        for (scheduleId in targets) {
            val activeHoldsForSchedule = holds.values.filter { it.scheduleId == scheduleId }
            val needed = TARGET_HOLDS_PER_SCHEDULE - activeHoldsForSchedule.size
            if (needed <= 0) continue

            // BookingRepository 의 PENDING 조회가 (userId, scheduleId) 기준이므로 회차별로 봇 중복만 피하면 된다.
            val busyBotsInSchedule = activeHoldsForSchedule.map { it.userId }.toMutableSet()
            val toCreate = minOf(needed, MAX_NEW_HOLDS_PER_SCHEDULE_PER_TICK)

            repeat(toCreate) {
                val idleBotId = botIds.filter { it !in busyBotsInSchedule }.randomOrNull() ?: return@repeat
                val seats = seatSupport.pickFreeSeats(scheduleId, Random.nextInt(1, 3)) ?: return@repeat

                try {
                    val response = bookingFacade.startBooking(scheduleId, seats, idleBotId)
                    busyBotsInSchedule.add(idleBotId)
                    holds[response.bookingId] = SimulatedHold(
                        bookingId = response.bookingId,
                        userId = idleBotId,
                        scheduleId = scheduleId,
                        settleAt = LocalDateTime.now().plus(
                            Duration.ofSeconds(Random.nextLong(MIN_HOLD_SECONDS, MAX_HOLD_SECONDS + 1))
                        )
                    )
                } catch (e: Exception) {
                    // 지난 회차/좌석 경합 등 - 다음 틱에 다시 시도
                    log.debug("[DemoSeed] 시뮬레이터 점유 시작 실패 (scheduleId={}): {}", scheduleId, e.message)
                }
            }
        }
    }

    private fun loadBotIds(): List<Long> {
        if (cachedBotIds.isEmpty()) {
            cachedBotIds = userSeedProvider.findBotUserIds()
        }
        return cachedBotIds
    }

    /** 현재 추적 중인 봇 점유 수 (모니터링/테스트용) */
    fun trackedHoldCount(): Int = holds.size
}

/**
 * 시뮬레이터가 만든 봇 점유 1건
 *
 * @property settleAt 이 시각 이후 확정/해제 처리
 */
data class SimulatedHold(
    val bookingId: UUID,
    val userId: Long,
    val scheduleId: Long,
    val settleAt: LocalDateTime
)
