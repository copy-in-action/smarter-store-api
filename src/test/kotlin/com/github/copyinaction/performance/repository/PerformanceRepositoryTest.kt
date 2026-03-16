package com.github.copyinaction.performance.repository

import com.github.copyinaction.config.QuerydslConfig
import com.github.copyinaction.performance.domain.Performance
import com.github.copyinaction.performance.dto.PerformanceSearchRequest
import com.github.copyinaction.performance.dto.PerformanceSearchSort
import com.github.copyinaction.performance.dto.PerformanceSearchStatus
import com.github.copyinaction.venue.domain.Venue
import com.github.copyinaction.venue.repository.VenueRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@DataJpaTest
@Import(QuerydslConfig::class, PerformanceRepositoryImpl::class)
@ActiveProfiles("local")
class PerformanceRepositoryTest {

    @Autowired
    private lateinit var performanceRepository: PerformanceRepository

    @Autowired
    private lateinit var venueRepository: VenueRepository

    @BeforeEach
    fun setUp() {
        val venue = venueRepository.save(
            Venue(
                name = "서울 예술의 전당",
                address = "서울특별시 서초구",
                seatingChart = "{}"
            )
        )

        // 1. 판매 중인 공연
        performanceRepository.save(
            Performance(
                title = "햄릿 - 서울",
                description = "세익스피어 4대 비극 중 하나",
                category = "연극",
                runningTime = 120,
                ageRating = "12세 이상",
                mainImageUrl = "hamlet.jpg",
                startDate = LocalDate.now().minusDays(10),
                endDate = LocalDate.now().plusDays(10),
                venue = venue,
                visible = true
            )
        )

        // 2. 판매 예정 공연
        performanceRepository.save(
            Performance(
                title = "지킬 앤 하이드",
                description = "선과 악의 대결",
                category = "뮤지컬",
                runningTime = 150,
                ageRating = "15세 이상",
                mainImageUrl = "jekyll.jpg",
                startDate = LocalDate.now().plusDays(20),
                endDate = LocalDate.now().plusDays(40),
                venue = venue,
                visible = true
            )
        )

        // 3. 종료된 공연
        performanceRepository.save(
            Performance(
                title = "라이온 킹",
                description = "정글의 왕 심바",
                category = "뮤지컬",
                runningTime = 160,
                ageRating = "전체 관람가",
                mainImageUrl = "lionking.jpg",
                startDate = LocalDate.now().minusDays(50),
                endDate = LocalDate.now().minusDays(30),
                venue = venue,
                visible = true
            )
        )
    }

    @Test
    @DisplayName("키워드로 공연을 검색할 수 있어야 한다")
    fun searchByKeywordTest() {
        val request = PerformanceSearchRequest(keyword = "햄릿")
        val result = performanceRepository.searchPerformances(request)

        assertEquals(1, result.content.size)
        assertTrue(result.content[0].title.contains("햄릿"))
    }

    @Test
    @DisplayName("카테고리 필터가 정상적으로 동작해야 한다")
    fun categoryFilterTest() {
        val request = PerformanceSearchRequest(category = listOf("뮤지컬"))
        val result = performanceRepository.searchPerformances(request)

        assertEquals(2, result.content.size)
        assertTrue(result.content.all { it.category == "뮤지컬" })
    }

    @Test
    @DisplayName("상태 필터(ON_SALE)가 정상적으로 동작해야 한다")
    fun statusFilterTest() {
        val request = PerformanceSearchRequest(status = listOf(PerformanceSearchStatus.ON_SALE))
        val result = performanceRepository.searchPerformances(request)

        assertEquals(1, result.content.size)
        assertEquals("햄릿 - 서울", result.content[0].title)
    }

    @Test
    @DisplayName("복합 필터(키워드 + 상태)가 정상적으로 동작해야 한다")
    fun multiFilterTest() {
        val request = PerformanceSearchRequest(
            keyword = "라이온",
            status = listOf(PerformanceSearchStatus.CLOSED)
        )
        val result = performanceRepository.searchPerformances(request)

        assertEquals(1, result.content.size)
        assertEquals("라이온 킹", result.content[0].title)
    }

    @Test
    @DisplayName("정렬(최신순)이 정상적으로 동작해야 한다")
    fun sortingTest() {
        val request = PerformanceSearchRequest(sort = PerformanceSearchSort.CREATED_AT_DESC)
        val result = performanceRepository.searchPerformances(request)

        // 저장한 순서의 역순 (라이온 킹이 가장 최근에 저장됨)
        assertEquals("라이온 킹", result.content[0].title)
        assertEquals("지킬 앤 하이드", result.content[1].title)
        assertEquals("햄릿 - 서울", result.content[2].title)
    }
}
