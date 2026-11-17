package com.github.copyinaction.demo.scheduler

import com.github.copyinaction.demo.config.ConditionalOnDemoSeed
import com.github.copyinaction.demo.service.DemoSeedOrchestrator
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import jakarta.annotation.PreDestroy
import java.util.concurrent.Executors

/**
 * 데모 시딩 기동 러너 + 정기 배치
 *
 * - 기동 시: 좌석배치도/공연장 매핑 -> 계정 -> 회차 롤링 + 초기 좌석 -> (비어 있으면) 공용 계정 샘플 예매
 * - 정기 배치: 주간 롤링, 일간 봇 정리, 공용 체험 계정 초기화
 */
@Component
@ConditionalOnDemoSeed
class DemoSeedRunner(
    private val orchestrator: DemoSeedOrchestrator
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 시딩 작업 전용 단일 스레드.
     * - 기동 시 초기 시딩은 수 분이 걸릴 수 있어 ApplicationRunner 에서 동기로 돌리면 헬스체크 통과가 늦어진다.
     * - Spring 기본 스케줄러 스레드는 1개라서, 오래 걸리는 배치를 @Scheduled 스레드에서 직접 돌리면
     *   SSE heartbeat / 만료 예매 정리 같은 다른 스케줄이 함께 밀린다.
     */
    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "demo-seed-worker").apply { isDaemon = true }
    }

    override fun run(args: ApplicationArguments) {
        submit("기동 시 초기 시딩") { orchestrator.runInitial() }
    }

    /** 주간 롤링 배치 (기본: 매주 월요일 03:00) */
    @Scheduled(cron = "\${demo.seed.rolling-cron:0 0 3 * * MON}")
    fun weeklyRolling() {
        submit("주간 롤링") { orchestrator.runWeeklyRolling() }
    }

    /** 일간 봇 이력 정리 배치 (기본: 매일 04:30) */
    @Scheduled(cron = "\${demo.seed.cleanup-cron:0 30 4 * * *}")
    fun dailyCleanup() {
        submit("봇 이력 정리") { orchestrator.cleanup() }
    }

    /** 공용 체험 계정 초기화 배치 (기본: 매일 04:00) */
    @Scheduled(cron = "\${demo.seed.demo-user-reset-cron:0 0 4 * * *}")
    fun resetDemoUsers() {
        submit("공용 체험 계정 초기화") { orchestrator.resetDemoUsers() }
    }

    @PreDestroy
    fun shutdown() {
        worker.shutdownNow()
    }

    private fun submit(taskName: String, task: () -> Unit) {
        worker.execute {
            try {
                task()
            } catch (e: Exception) {
                log.error("[DemoSeed] {} 실패", taskName, e)
            }
        }
    }
}
