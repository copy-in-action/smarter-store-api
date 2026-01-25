package com.github.copyinaction.home.domain

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 홈 화면 섹션별 하위 태그
 */
enum class HomeSectionTag(
    val section: HomeSection,
    val displayName: String,
    val displayOrder: Int
) {
    // ===== 인기티켓 (POPULAR_TICKET) =====
    @Schema(description = "인기티켓 > 금주오픈티켓")
    WEEKLY_OPEN(HomeSection.POPULAR_TICKET, "금주오픈티켓", 1),

    @Schema(description = "인기티켓 > 뮤지컬")
    MUSICAL(HomeSection.POPULAR_TICKET, "뮤지컬", 2),

    @Schema(description = "인기티켓 > 콘서트")
    CONCERT(HomeSection.POPULAR_TICKET, "콘서트", 3),

    @Schema(description = "인기티켓 > 연극")
    THEATER(HomeSection.POPULAR_TICKET, "연극", 4),

    @Schema(description = "인기티켓 > 전시/행사")
    EXHIBITION(HomeSection.POPULAR_TICKET, "전시/행사", 5),

    // ===== 데이트코스 (DATE_COURSE) =====
    @Schema(description = "데이트코스 > 뮤지컬")
    DATE_MUSICAL(HomeSection.DATE_COURSE, "뮤지컬", 1),

    @Schema(description = "데이트코스 > 연극")
    DATE_THEATER(HomeSection.DATE_COURSE, "연극", 2),

    @Schema(description = "데이트코스 > 클래식")
    DATE_CLASSIC(HomeSection.DATE_COURSE, "클래식", 3),

    @Schema(description = "데이트코스 > 전시")
    DATE_EXHIBITION(HomeSection.DATE_COURSE, "전시", 4),

    // ===== 이런 티켓은 어때요? (RECOMMENDED) =====
    @Schema(description = "이런 티켓은 어때요? > 한정특가")
    LIMITED_SALE(HomeSection.RECOMMENDED, "한정특가", 1),

    @Schema(description = "이런 티켓은 어때요? > 아이와 함께")
    WITH_KIDS(HomeSection.RECOMMENDED, "아이와 함께", 2),

    @Schema(description = "이런 티켓은 어때요? > 대학로공연")
    DAEHAKRO(HomeSection.RECOMMENDED, "대학로공연", 3),

    // ===== 어디로 떠나볼까요? (REGION) - 자동매핑 =====
    @Schema(description = "어디로 떠나볼까요? > 서울")
    REGION_SEOUL(HomeSection.REGION, "서울", 1),

    @Schema(description = "어디로 떠나볼까요? > 경기")
    REGION_GYEONGGI(HomeSection.REGION, "경기", 2),

    @Schema(description = "어디로 떠나볼까요? > 부산")
    REGION_BUSAN(HomeSection.REGION, "부산", 3),

    @Schema(description = "어디로 떠나볼까요? > 대구")
    REGION_DAEGU(HomeSection.REGION, "대구", 4),

    @Schema(description = "어디로 떠나볼까요? > 대전")
    REGION_DAEJEON(HomeSection.REGION, "대전", 5),

    @Schema(description = "어디로 떠나볼까요? > 전국")
    REGION_NATIONWIDE(HomeSection.REGION, "전국", 6);

    companion object {
        fun getBySection(section: HomeSection): List<HomeSectionTag> =
            entries.filter { it.section == section }
                .sortedBy { it.displayOrder }

        fun getRegionTags(): List<HomeSectionTag> =
            getBySection(HomeSection.REGION)
    }
}
