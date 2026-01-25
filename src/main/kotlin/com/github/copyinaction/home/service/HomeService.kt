package com.github.copyinaction.home.service

import com.github.copyinaction.home.domain.HomeSection
import com.github.copyinaction.home.domain.HomeSectionTag
import com.github.copyinaction.home.dto.*
import com.github.copyinaction.home.repository.PerformanceHomeTagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class HomeService(
    private val performanceHomeTagRepository: PerformanceHomeTagRepository
) {

    /**
     * 홈 화면 전체 섹션 조회
     * - visible=true인 공연만 노출
     * - 섹션 > 태그 > 공연 구조로 반환
     */
    fun getAllSections(): HomeSectionsResponse {
        val sections = HomeSection.getAllSorted().map { section ->
            val tags = HomeSectionTag.getBySection(section).map { tag ->
                val performances = performanceHomeTagRepository
                    .findByTagAndVisibleOrderByDisplayOrderAsc(tag)
                    .map { HomePerformanceResponse.from(it) }

                HomeTagWithPerformancesResponse(
                    tag = tag,
                    displayName = tag.displayName,
                    displayOrder = tag.displayOrder,
                    performances = performances
                )
            }

            HomeSectionResponse(
                section = section,
                displayName = section.displayName,
                displayOrder = section.displayOrder,
                tags = tags
            )
        }

        return HomeSectionsResponse(sections = sections)
    }

    /**
     * 특정 태그의 공연 목록 조회
     * - visible=true인 공연만 노출
     */
    fun getPerformancesByTag(tag: HomeSectionTag): HomeTagWithPerformancesResponse {
        val performances = performanceHomeTagRepository
            .findByTagAndVisibleOrderByDisplayOrderAsc(tag)
            .map { HomePerformanceResponse.from(it) }

        return HomeTagWithPerformancesResponse(
            tag = tag,
            displayName = tag.displayName,
            displayOrder = tag.displayOrder,
            performances = performances
        )
    }

    /**
     * 특정 섹션의 태그 및 공연 목록 조회
     * - visible=true인 공연만 노출
     */
    fun getSectionWithPerformances(section: HomeSection): HomeSectionResponse {
        val tags = HomeSectionTag.getBySection(section).map { tag ->
            val performances = performanceHomeTagRepository
                .findByTagAndVisibleOrderByDisplayOrderAsc(tag)
                .map { HomePerformanceResponse.from(it) }

            HomeTagWithPerformancesResponse(
                tag = tag,
                displayName = tag.displayName,
                displayOrder = tag.displayOrder,
                performances = performances
            )
        }

        return HomeSectionResponse(
            section = section,
            displayName = section.displayName,
            displayOrder = section.displayOrder,
            tags = tags
        )
    }
}
