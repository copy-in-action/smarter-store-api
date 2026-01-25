package com.github.copyinaction.home.service

import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.home.domain.HomeSection
import com.github.copyinaction.home.domain.HomeSectionTag
import com.github.copyinaction.home.domain.PerformanceHomeTag
import com.github.copyinaction.home.domain.RegionMapper
import com.github.copyinaction.home.dto.*
import com.github.copyinaction.home.repository.PerformanceHomeTagRepository
import com.github.copyinaction.performance.repository.PerformanceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PerformanceHomeTagService(
    private val performanceHomeTagRepository: PerformanceHomeTagRepository,
    private val performanceRepository: PerformanceRepository
) {

    /**
     * 공연에 홈 태그 추가
     */
    @Transactional
    fun addTag(performanceId: Long, request: AddHomeTagRequest): PerformanceHomeTagResponse {
        val performance = performanceRepository.findById(performanceId)
            .orElseThrow { CustomException(ErrorCode.PERFORMANCE_NOT_FOUND) }

        // 지역 태그는 수동 추가 불가 (자동 태깅만 허용)
        if (RegionMapper.isRegionTag(request.tag)) {
            throw CustomException(ErrorCode.REGION_TAG_MANUAL_NOT_ALLOWED)
        }

        // 중복 체크
        if (performanceHomeTagRepository.existsByPerformanceIdAndTag(performanceId, request.tag)) {
            throw CustomException(ErrorCode.HOME_TAG_ALREADY_EXISTS)
        }

        // displayOrder 결정: 요청값이 없으면 마지막 순서 + 1
        val displayOrder = request.displayOrder
            ?: (performanceHomeTagRepository.findMaxDisplayOrderByTag(request.tag) + 1)

        val homeTag = PerformanceHomeTag.create(
            performance = performance,
            tag = request.tag,
            displayOrder = displayOrder,
            isAutoTagged = false
        )

        val saved = performanceHomeTagRepository.save(homeTag)
        return PerformanceHomeTagResponse.from(saved)
    }

    /**
     * 공연의 홈 태그 목록 조회
     */
    fun getTagsByPerformance(performanceId: Long): List<PerformanceHomeTagResponse> {
        if (!performanceRepository.existsById(performanceId)) {
            throw CustomException(ErrorCode.PERFORMANCE_NOT_FOUND)
        }

        return performanceHomeTagRepository.findByPerformanceId(performanceId)
            .map { PerformanceHomeTagResponse.from(it) }
    }

    /**
     * 공연의 홈 태그 삭제
     */
    @Transactional
    fun removeTag(performanceId: Long, tag: HomeSectionTag) {
        if (!performanceRepository.existsById(performanceId)) {
            throw CustomException(ErrorCode.PERFORMANCE_NOT_FOUND)
        }

        if (!performanceHomeTagRepository.existsByPerformanceIdAndTag(performanceId, tag)) {
            throw CustomException(ErrorCode.HOME_TAG_NOT_FOUND)
        }

        performanceHomeTagRepository.deleteByPerformanceIdAndTag(performanceId, tag)
    }

    /**
     * 태그별 공연 목록 조회 (관리자용 - visible 무관)
     */
    fun getPerformancesByTag(tag: HomeSectionTag): List<TagPerformanceResponse> {
        return performanceHomeTagRepository.findByTagOrderByDisplayOrderAsc(tag)
            .map { TagPerformanceResponse.from(it) }
    }

    /**
     * 태그 내 공연 순서 변경
     */
    @Transactional
    fun updateDisplayOrders(tag: HomeSectionTag, request: UpdateDisplayOrderRequest) {
        val existingTags = performanceHomeTagRepository.findByTagOrderByDisplayOrderAsc(tag)
            .associateBy { it.performance.id }

        request.performanceOrders.forEach { orderItem ->
            val homeTag = existingTags[orderItem.performanceId]
                ?: throw CustomException(
                    ErrorCode.HOME_TAG_NOT_FOUND,
                    "태그 ${tag.displayName}에 공연 ID ${orderItem.performanceId}가 없습니다."
                )
            homeTag.updateDisplayOrder(orderItem.displayOrder)
        }
    }

    /**
     * 공연의 지역 태그 자동 갱신 (venue 변경 시 호출)
     */
    @Transactional
    fun refreshRegionTag(performanceId: Long) {
        val performance = performanceRepository.findById(performanceId)
            .orElseThrow { CustomException(ErrorCode.PERFORMANCE_NOT_FOUND) }

        // 기존 자동 태그 삭제
        performanceHomeTagRepository.deleteAutoTagsByPerformanceId(performanceId)

        // venue가 있으면 새 지역 태그 추가
        val venue = performance.venue ?: return
        val regionTag = RegionMapper.mapFromAddress(venue.address)

        val displayOrder = performanceHomeTagRepository.findMaxDisplayOrderByTag(regionTag) + 1

        val homeTag = PerformanceHomeTag.create(
            performance = performance,
            tag = regionTag,
            displayOrder = displayOrder,
            isAutoTagged = true
        )

        performanceHomeTagRepository.save(homeTag)
    }

    /**
     * 전체 섹션/태그 메타 정보 조회
     */
    fun getAllSectionsMetadata(): List<SectionMetadataResponse> {
        return HomeSection.getAllSorted().map { section ->
            SectionMetadataResponse(
                section = section,
                displayName = section.displayName,
                displayOrder = section.displayOrder,
                tags = HomeSectionTag.getBySection(section).map { tag ->
                    TagMetadataResponse(
                        tag = tag,
                        displayName = tag.displayName,
                        displayOrder = tag.displayOrder,
                        isRegionTag = RegionMapper.isRegionTag(tag)
                    )
                }
            )
        }
    }
}

// 메타데이터 응답 DTO
data class SectionMetadataResponse(
    val section: HomeSection,
    val displayName: String,
    val displayOrder: Int,
    val tags: List<TagMetadataResponse>
)

data class TagMetadataResponse(
    val tag: HomeSectionTag,
    val displayName: String,
    val displayOrder: Int,
    val isRegionTag: Boolean
)
