package com.github.copyinaction.home.controller

import com.github.copyinaction.home.domain.HomeSection
import com.github.copyinaction.home.domain.HomeSectionTag
import com.github.copyinaction.home.dto.HomeSectionResponse
import com.github.copyinaction.home.dto.HomeSectionsResponse
import com.github.copyinaction.home.dto.HomeTagWithPerformancesResponse
import com.github.copyinaction.home.service.HomeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "home", description = "홈 화면 API - 섹션별 공연 목록 조회")
@RestController
@RequestMapping("/api/home")
class HomeController(
    private val homeService: HomeService
) {

    @Operation(
        summary = "홈 전체 섹션 조회",
        description = """
홈 화면에 표시할 전체 섹션과 공연 목록을 조회합니다.

**섹션 구조:**
1. 인기티켓 (금주오픈티켓, 뮤지컬, 콘서트, 연극, 전시/행사)
2. 데이트코스 (뮤지컬, 연극, 클래식, 전시)
3. 이런 티켓은 어때요? (한정특가, 아이와 함께, 대학로공연)
4. 어디로 떠나볼까요? (서울, 경기, 부산, 대구, 대전, 전국)

**참고:**
- visible=true인 공연만 노출됩니다.
- 각 태그 내 공연은 관리자가 지정한 순서대로 정렬됩니다.
        """
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공")
    )
    @GetMapping("/sections")
    fun getAllSections(): ResponseEntity<HomeSectionsResponse> {
        return ResponseEntity.ok(homeService.getAllSections())
    }

    @Operation(
        summary = "특정 섹션 조회",
        description = "홈 화면의 특정 섹션과 해당 섹션의 태그별 공연 목록을 조회합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공")
    )
    @GetMapping("/sections/{section}")
    fun getSection(
        @Parameter(description = "섹션", required = true, example = "POPULAR_TICKET")
        @PathVariable section: HomeSection
    ): ResponseEntity<HomeSectionResponse> {
        return ResponseEntity.ok(homeService.getSectionWithPerformances(section))
    }

    @Operation(
        summary = "특정 태그의 공연 목록 조회",
        description = "홈 화면의 특정 태그에 등록된 공연 목록을 조회합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공")
    )
    @GetMapping("/tags/{tag}/performances")
    fun getPerformancesByTag(
        @Parameter(description = "태그", required = true, example = "MUSICAL")
        @PathVariable tag: HomeSectionTag
    ): ResponseEntity<HomeTagWithPerformancesResponse> {
        return ResponseEntity.ok(homeService.getPerformancesByTag(tag))
    }
}
