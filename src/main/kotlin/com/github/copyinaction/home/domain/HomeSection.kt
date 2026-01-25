package com.github.copyinaction.home.domain

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 홈 화면 대분류 섹션
 */
enum class HomeSection(
    val displayName: String,
    val displayOrder: Int
) {
    @Schema(description = "인기티켓")
    POPULAR_TICKET("인기티켓", 1),

    @Schema(description = "데이트코스")
    DATE_COURSE("데이트코스", 2),

    @Schema(description = "이런 티켓은 어때요?")
    RECOMMENDED("이런 티켓은 어때요?", 3),

    @Schema(description = "어디로 떠나볼까요?")
    REGION("어디로 떠나볼까요?", 4);

    companion object {
        fun getAllSorted(): List<HomeSection> =
            entries.sortedBy { it.displayOrder }
    }
}
