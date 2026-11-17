package com.github.copyinaction.demo.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.copyinaction.demo.config.ConditionalOnDemoSeed
import com.github.copyinaction.home.domain.HomeSectionTag
import com.github.copyinaction.home.domain.RegionMapper
import com.github.copyinaction.home.repository.PerformanceHomeTagRepository
import com.github.copyinaction.home.service.PerformanceHomeTagService
import com.github.copyinaction.performance.repository.PerformanceRepository
import com.github.copyinaction.venue.domain.SeatCapacityCommand
import com.github.copyinaction.venue.domain.SeatingChartUpdatedEvent
import com.github.copyinaction.venue.repository.VenueRepository
import com.github.copyinaction.venue.util.SeatingChartParser
import org.slf4j.LoggerFactory
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import kotlin.random.Random

/**
 * [Track 0] 공연장 좌석배치도 + 공연-공연장 매핑 시딩
 *
 * 1. 좌석배치도가 없는 공연장에 5종 템플릿 중 하나를 랜덤 배정
 * 2. 공연장이 없는 공연을 좌석배치도가 있는 공연장에 매핑 (지역 태그가 있으면 같은 지역 우선)
 *
 * 이미 좌석배치도가 있는 공연장과 이미 공연장이 있는 공연은 절대 건드리지 않는다.
 * (좌석배치도 변경 이벤트가 같은 트랜잭션에서 TicketOption 동기화를 수행하기 때문)
 */
@Service
@ConditionalOnDemoSeed
class DemoVenueSeatingSeedService(
    private val venueRepository: VenueRepository,
    private val performanceRepository: PerformanceRepository,
    private val performanceHomeTagRepository: PerformanceHomeTagRepository,
    private val performanceHomeTagService: PerformanceHomeTagService,
    private val seatingChartParser: SeatingChartParser,
    private val objectMapper: ObjectMapper,
    transactionManager: PlatformTransactionManager
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 공연장/공연 단위로 트랜잭션을 분리해 한 건의 오류가 전체를 롤백하지 않도록 한다. */
    private val txTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    /**
     * 클래스패스의 좌석배치도 템플릿(JSON)을 컴팩트 문자열로 로드한다.
     */
    private val templates: List<String> by lazy { loadTemplates() }

    /**
     * 0-1. 좌석배치도가 없는 공연장에 템플릿 배정
     * @return 좌석배치도가 새로 배정된 공연장 수
     */
    fun seedSeatingCharts(): Int {
        val targetVenueIds = venueRepository.findAll()
            .filter { it.seatingChart.isNullOrBlank() }
            .map { it.id }

        if (targetVenueIds.isEmpty()) {
            log.info("[DemoSeed] 좌석배치도 시딩 대상 공연장 없음")
            return 0
        }
        if (templates.isEmpty()) {
            log.error("[DemoSeed] 좌석배치도 템플릿(classpath:demo/seating-charts/*.json)을 찾을 수 없습니다.")
            return 0
        }

        var seeded = 0
        for (venueId in targetVenueIds) {
            try {
                val done = txTemplate.execute { assignTemplate(venueId) } ?: false
                if (done) seeded++
            } catch (e: Exception) {
                log.warn("[DemoSeed] 공연장 {} 좌석배치도 시딩 실패: {}", venueId, e.message, e)
            }
        }
        log.info("[DemoSeed] 공연장 좌석배치도 시딩 완료: {}/{}", seeded, targetVenueIds.size)
        return seeded
    }

    private fun assignTemplate(venueId: Long): Boolean {
        val venue = venueRepository.findById(venueId).orElse(null) ?: return false
        // 동시 실행 방어: 다른 경로에서 이미 배치도가 채워졌다면 건드리지 않는다.
        if (!venue.seatingChart.isNullOrBlank()) return false

        val chartJson = templates[Random.nextInt(templates.size)]
        val commands = seatingChartParser.countSeatsByGrade(chartJson)
            .filterValues { it > 0 }
            .map { (grade, count) -> SeatCapacityCommand(grade, count) }

        venue.updateSeatingChart(chartJson, commands)
        venue.registerEvent(SeatingChartUpdatedEvent(venue.id, chartJson))
        venueRepository.save(venue)
        return true
    }

    /**
     * 0-2. 공연장이 없는 공연을 좌석배치도가 있는 공연장에 매핑
     * @return 공연장이 새로 매핑된 공연 수
     */
    fun mapPerformancesToVenues(): Int {
        val targetPerformanceIds = performanceRepository.findAll()
            .filter { it.venue == null }
            .map { it.id }

        if (targetPerformanceIds.isEmpty()) {
            log.info("[DemoSeed] 공연장 매핑 대상 공연 없음")
            return 0
        }

        val venuePool = venueRepository.findAll()
            .filter { !it.seatingChart.isNullOrBlank() }
            .map { VenueRef(it.id, it.address) }
            .sortedBy { it.id }

        if (venuePool.isEmpty()) {
            log.warn("[DemoSeed] 좌석배치도가 있는 공연장이 없어 공연 매핑을 건너뜁니다.")
            return 0
        }

        var roundRobin = 0
        var mapped = 0
        for (performanceId in targetPerformanceIds) {
            try {
                val done = txTemplate.execute {
                    val preferred = findRegionPreferredVenues(performanceId, venuePool)
                    val venueRef = if (preferred.isNotEmpty()) {
                        preferred[Random.nextInt(preferred.size)]
                    } else {
                        venuePool[roundRobin % venuePool.size]
                    }
                    assignVenue(performanceId, venueRef.id)
                } ?: false
                if (done) {
                    mapped++
                    roundRobin++
                }
            } catch (e: Exception) {
                log.warn("[DemoSeed] 공연 {} 공연장 매핑 실패: {}", performanceId, e.message, e)
            }
        }
        log.info("[DemoSeed] 공연-공연장 매핑 완료: {}/{}", mapped, targetPerformanceIds.size)
        return mapped
    }

    private fun assignVenue(performanceId: Long, venueId: Long): Boolean {
        val performance = performanceRepository.findById(performanceId).orElse(null) ?: return false
        if (performance.venue != null) return false
        val venue = venueRepository.findById(venueId).orElse(null) ?: return false

        performance.assignVenue(venue)
        // 지역 태그(REGION_*)는 공연장 주소 기반 자동 태깅이므로 매핑 직후 갱신
        performanceHomeTagService.refreshRegionTag(performanceId)
        return true
    }

    /**
     * 공연에 이미 지역 태그(REGION_*)가 있다면 주소가 같은 지역으로 매핑되는 공연장만 우선 후보로 반환
     */
    private fun findRegionPreferredVenues(performanceId: Long, venuePool: List<VenueRef>): List<VenueRef> {
        val regionTag = performanceHomeTagRepository.findByPerformanceId(performanceId)
            .map { it.tag }
            .firstOrNull { RegionMapper.isRegionTag(it) && it != HomeSectionTag.REGION_NATIONWIDE }
            ?: return emptyList()

        return venuePool.filter { RegionMapper.mapFromAddress(it.address) == regionTag }
    }

    private fun loadTemplates(): List<String> {
        return try {
            PathMatchingResourcePatternResolver()
                .getResources("classpath:demo/seating-charts/*.json")
                .sortedBy { it.filename }
                .map { resource ->
                    resource.inputStream.use { input ->
                        // 저장 형식을 VenueService.updateSeatingChart 와 동일한 컴팩트 JSON으로 통일
                        objectMapper.writeValueAsString(objectMapper.readTree(input))
                    }
                }
        } catch (e: Exception) {
            log.error("[DemoSeed] 좌석배치도 템플릿 로드 실패", e)
            emptyList()
        }
    }

    private data class VenueRef(val id: Long, val address: String?)
}
