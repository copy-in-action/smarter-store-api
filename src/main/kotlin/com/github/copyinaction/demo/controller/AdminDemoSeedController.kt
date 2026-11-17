package com.github.copyinaction.demo.controller

import com.github.copyinaction.demo.config.ConditionalOnDemoSeed
import com.github.copyinaction.demo.service.DemoSeedOrchestrator
import com.github.copyinaction.demo.service.DemoSeedResult
import com.github.copyinaction.demo.service.DemoSeedStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.Executors

/**
 * 데모 데이터 시딩 수동 실행 API (관리자 전용)
 * `demo.seed.enabled=true` 일 때만 노출된다.
 */
@Tag(name = "admin-demo-seed", description = "관리자용 데모 데이터 시딩 API")
@RestController
@RequestMapping("/api/admin/demo/seed")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@ConditionalOnDemoSeed
class AdminDemoSeedController(
    private val orchestrator: DemoSeedOrchestrator
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 회차 롤링은 오래 걸릴 수 있어 HTTP 요청 스레드를 점유하지 않도록 별도 스레드에서 실행한다. */
    private val asyncExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "demo-seed-admin").apply { isDaemon = true }
    }

    @Operation(
        summary = "공연장 좌석배치도 + 공연-공연장 매핑 시딩",
        description = "좌석배치도가 없는 공연장에 템플릿을 배정하고, 공연장이 없는 공연을 공연장에 매핑합니다.\n\n**권한: ADMIN**"
    )
    @PostMapping("/venues")
    fun seedVenues(): ResponseEntity<DemoSeedResult> = respond(orchestrator.seedVenues())

    @Operation(
        summary = "8주 회차 롤링 + 초기 좌석 시딩 (비동기)",
        description = "백그라운드에서 실행되며 즉시 202를 반환합니다. 진행 상황은 서버 로그(`[DemoSeed]`)로 확인합니다.\n\n**권한: ADMIN**"
    )
    @PostMapping("/schedules")
    fun seedSchedules(): ResponseEntity<Map<String, String>> {
        asyncExecutor.execute {
            try {
                orchestrator.seedSchedules()
            } catch (e: Exception) {
                log.error("[DemoSeed] 관리자 요청 회차 시딩 실패", e)
            }
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(mapOf("task" to "schedules", "status" to "ACCEPTED"))
    }

    @Operation(
        summary = "공용 체험 계정 초기화 + 재시딩",
        description = "demo-user-01~03 의 예매/결제 내역을 모두 삭제하고 샘플 예매를 다시 생성합니다.\n\n**권한: ADMIN**"
    )
    @PostMapping("/demo-users/reset")
    fun resetDemoUsers(): ResponseEntity<DemoSeedResult> = respond(orchestrator.resetDemoUsers())

    @Operation(
        summary = "봇 이력 정리",
        description = "봇의 비활성 예매, 보관 기간이 지난 과거 회차 이력을 정리하고 과점유 회차를 조정합니다.\n\n**권한: ADMIN**"
    )
    @PostMapping("/cleanup")
    fun cleanup(): ResponseEntity<DemoSeedResult> = respond(orchestrator.cleanup())

    private fun respond(result: DemoSeedResult): ResponseEntity<DemoSeedResult> {
        val status = when (result.status) {
            DemoSeedStatus.COMPLETED -> HttpStatus.OK
            DemoSeedStatus.SKIPPED_BUSY -> HttpStatus.CONFLICT
            DemoSeedStatus.FAILED -> HttpStatus.INTERNAL_SERVER_ERROR
        }
        return ResponseEntity.status(status).body(result)
    }
}
