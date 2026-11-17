package com.github.copyinaction.demo.service

import com.github.copyinaction.demo.config.ConditionalOnDemoSeed
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.locks.ReentrantLock

/**
 * 데모 시딩 유스케이스 오케스트레이터
 *
 * 기동 러너, 스케줄러, 관리자 API가 모두 이 클래스를 통해 시딩을 실행한다.
 * 시딩 작업은 서로 같은 테이블(회차/좌석/예매)을 대량으로 만지므로
 * 동시에 두 개가 돌지 않도록 하나의 락으로 직렬화한다. (이미 실행 중이면 건너뜀)
 */
@Service
@ConditionalOnDemoSeed
class DemoSeedOrchestrator(
    private val venueSeatingSeedService: DemoVenueSeatingSeedService,
    private val userSeedProvider: DemoUserSeedProvider,
    private val scheduleRollingService: DemoScheduleRollingService,
    private val bookingSeedService: DemoBookingSeedService,
    private val userBookingSeedService: DemoUserBookingSeedService,
    private val cleanupService: DemoSeedCleanupService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val lock = ReentrantLock()

    /**
     * Track 0: 공연장 좌석배치도 + 공연-공연장 매핑
     */
    fun seedVenues(): DemoSeedResult = runExclusive("venues") {
        val charts = venueSeatingSeedService.seedSeatingCharts()
        val mapped = venueSeatingSeedService.mapPerformancesToVenues()
        mapOf("venueSeatingChartsSeeded" to charts, "performancesMappedToVenue" to mapped)
    }

    /**
     * Track 1: 8주 롤링 회차 생성 + 신규 회차 초기 좌석 시딩 (봇/공용 계정 보장 포함)
     */
    fun seedSchedules(): DemoSeedResult = runExclusive("schedules") {
        doSeedSchedules()
    }

    /**
     * 공용 체험 계정 즉시 초기화 + 샘플 재시딩
     */
    fun resetDemoUsers(): DemoSeedResult = runExclusive("demo-users-reset") {
        userSeedProvider.ensureUsers()
        mapOf("sampleBookingsCreated" to userBookingSeedService.resetAndReseed())
    }

    /**
     * 봇 이력 정리 + 공용 계정 예매 상한 보호
     */
    fun cleanup(): DemoSeedResult = runExclusive("cleanup") {
        val result = cleanupService.cleanup()
        val capped = userBookingSeedService.enforceBookingCap()
        mapOf(
            "inactiveBotBookingsDeleted" to result.inactiveBotBookingsDeleted,
            "pastBotBookingsDeleted" to result.pastBotBookingsDeleted,
            "overbookedBookingsReleased" to result.overbookedBookingsReleased,
            "demoUserBookingsTrimmed" to capped
        )
    }

    /**
     * 주간 롤링 배치: 신규 공연/공연장도 반영되도록 Track 0 -> Track 1 순서로 실행
     */
    fun runWeeklyRolling(): DemoSeedResult = runExclusive("weekly-rolling") {
        val charts = venueSeatingSeedService.seedSeatingCharts()
        val mapped = venueSeatingSeedService.mapPerformancesToVenues()
        mapOf(
            "venueSeatingChartsSeeded" to charts,
            "performancesMappedToVenue" to mapped
        ) + doSeedSchedules()
    }

    /**
     * 서버 기동 시 1회 실행: Track 0 -> 계정 -> Track 1 -> (공용 계정 내역이 비어 있을 때만) 샘플 예매
     */
    fun runInitial(): DemoSeedResult = runExclusive("initial") {
        val details = linkedMapOf<String, Int>()

        details["venueSeatingChartsSeeded"] = venueSeatingSeedService.seedSeatingCharts()
        details["performancesMappedToVenue"] = venueSeatingSeedService.mapPerformancesToVenues()
        details.putAll(doSeedSchedules())

        if (!userBookingSeedService.hasAnyBooking()) {
            details["sampleBookingsCreated"] = userBookingSeedService.resetAndReseed()
        }
        details
    }

    private fun doSeedSchedules(): Map<String, Int> {
        userSeedProvider.ensureUsers()
        val botIds = userSeedProvider.findBotUserIds()

        val rolled = scheduleRollingService.rollAll()
        val newScheduleIds = rolled.values.flatten()
        val seats = bookingSeedService.seedSchedules(newScheduleIds, botIds)

        return mapOf(
            "performancesRolled" to rolled.size,
            "schedulesCreated" to newScheduleIds.size,
            "seatsSeeded" to seats
        )
    }

    /**
     * 락을 잡고 작업을 실행한다. 다른 시딩 작업이 진행 중이면 실행하지 않고 SKIPPED_BUSY 로 응답한다.
     */
    private fun runExclusive(task: String, action: () -> Map<String, Int>): DemoSeedResult {
        if (!lock.tryLock()) {
            log.warn("[DemoSeed] '{}' 작업 요청이 들어왔지만 다른 시딩 작업이 실행 중이라 건너뜁니다.", task)
            return DemoSeedResult(task, DemoSeedStatus.SKIPPED_BUSY, emptyMap())
        }
        val startedAt = LocalDateTime.now()
        return try {
            log.info("[DemoSeed] '{}' 작업 시작", task)
            val details = action()
            log.info("[DemoSeed] '{}' 작업 완료: {}", task, details)
            DemoSeedResult(task, DemoSeedStatus.COMPLETED, details, startedAt, LocalDateTime.now())
        } catch (e: Exception) {
            log.error("[DemoSeed] '{}' 작업 실패: {}", task, e.message, e)
            DemoSeedResult(task, DemoSeedStatus.FAILED, emptyMap(), startedAt, LocalDateTime.now(), e.message)
        } finally {
            lock.unlock()
        }
    }
}

enum class DemoSeedStatus { COMPLETED, SKIPPED_BUSY, FAILED }

/**
 * 시딩 작업 실행 결과
 */
data class DemoSeedResult(
    val task: String,
    val status: DemoSeedStatus,
    val details: Map<String, Int>,
    val startedAt: LocalDateTime? = null,
    val finishedAt: LocalDateTime? = null,
    val errorMessage: String? = null
)
