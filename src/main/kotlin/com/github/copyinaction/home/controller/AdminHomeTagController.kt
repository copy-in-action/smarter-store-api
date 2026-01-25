package com.github.copyinaction.home.controller

import com.github.copyinaction.common.exception.ErrorResponse
import com.github.copyinaction.home.domain.HomeSectionTag
import com.github.copyinaction.home.dto.*
import com.github.copyinaction.home.service.PerformanceHomeTagService
import com.github.copyinaction.home.service.SectionMetadataResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

@Tag(name = "admin-home-tag", description = "관리자용 홈 태그 API - 홈 화면 공연 배치 관리")
@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "bearerAuth")
class AdminHomeTagController(
    private val performanceHomeTagService: PerformanceHomeTagService
) {

    @Operation(
        summary = "섹션/태그 메타 정보 조회",
        description = "홈 화면의 모든 섹션과 태그 목록을 조회합니다.\n\n**권한: ADMIN**"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공")
    )
    @GetMapping("/home/sections/metadata")
    fun getSectionsMetadata(): ResponseEntity<List<SectionMetadataResponse>> {
        return ResponseEntity.ok(performanceHomeTagService.getAllSectionsMetadata())
    }

    @Operation(
        summary = "공연에 홈 태그 추가",
        description = """
공연을 홈 화면의 특정 태그에 추가합니다.

**권한: ADMIN**

**주의사항:**
- 지역 태그(REGION_*)는 수동 추가 불가 (공연장 주소 기반 자동 태깅)
- 동일 태그 중복 추가 불가
        """
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "태그 추가 성공"),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (지역 태그 수동 추가 시도)",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "공연을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409",
            description = "이미 등록된 태그",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/performances/{performanceId}/home-tags")
    fun addHomeTag(
        @Parameter(description = "공연 ID", required = true, example = "1")
        @PathVariable performanceId: Long,
        @Valid @RequestBody request: AddHomeTagRequest
    ): ResponseEntity<PerformanceHomeTagResponse> {
        val response = performanceHomeTagService.addTag(performanceId, request)
        val location = URI.create("/api/admin/performances/$performanceId/home-tags/${request.tag}")
        return ResponseEntity.created(location).body(response)
    }

    @Operation(
        summary = "공연의 홈 태그 목록 조회",
        description = "특정 공연에 등록된 모든 홈 태그를 조회합니다.\n\n**권한: ADMIN**"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(
            responseCode = "404",
            description = "공연을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping("/performances/{performanceId}/home-tags")
    fun getHomeTagsByPerformance(
        @Parameter(description = "공연 ID", required = true, example = "1")
        @PathVariable performanceId: Long
    ): ResponseEntity<List<PerformanceHomeTagResponse>> {
        return ResponseEntity.ok(performanceHomeTagService.getTagsByPerformance(performanceId))
    }

    @Operation(
        summary = "공연의 홈 태그 삭제",
        description = "공연에서 특정 홈 태그를 제거합니다.\n\n**권한: ADMIN**"
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "삭제 성공"),
        ApiResponse(
            responseCode = "404",
            description = "공연 또는 태그를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @DeleteMapping("/performances/{performanceId}/home-tags/{tag}")
    fun removeHomeTag(
        @Parameter(description = "공연 ID", required = true, example = "1")
        @PathVariable performanceId: Long,
        @Parameter(description = "삭제할 태그", required = true, example = "MUSICAL")
        @PathVariable tag: HomeSectionTag
    ): ResponseEntity<Unit> {
        performanceHomeTagService.removeTag(performanceId, tag)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "태그별 공연 목록 조회",
        description = "특정 태그에 등록된 모든 공연을 순서대로 조회합니다.\n\n**권한: ADMIN**"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공")
    )
    @GetMapping("/home-tags/{tag}/performances")
    fun getPerformancesByTag(
        @Parameter(description = "태그", required = true, example = "MUSICAL")
        @PathVariable tag: HomeSectionTag
    ): ResponseEntity<List<TagPerformanceResponse>> {
        return ResponseEntity.ok(performanceHomeTagService.getPerformancesByTag(tag))
    }

    @Operation(
        summary = "태그 내 공연 순서 변경",
        description = "태그 내 공연들의 노출 순서를 일괄 변경합니다.\n\n**권한: ADMIN**"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "순서 변경 성공"),
        ApiResponse(
            responseCode = "404",
            description = "태그에 해당 공연이 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PatchMapping("/home-tags/{tag}/performances/order")
    fun updateDisplayOrder(
        @Parameter(description = "태그", required = true, example = "MUSICAL")
        @PathVariable tag: HomeSectionTag,
        @Valid @RequestBody request: UpdateDisplayOrderRequest
    ): ResponseEntity<Unit> {
        performanceHomeTagService.updateDisplayOrders(tag, request)
        return ResponseEntity.ok().build()
    }
}
