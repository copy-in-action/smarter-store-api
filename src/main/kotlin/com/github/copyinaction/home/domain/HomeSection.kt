package com.github.copyinaction.home.domain

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 홈 화면 대분류 섹션
 */
@Schema(
    description = "홈 화면 대분류 섹션",
    enumAsRef = true
)
enum class HomeSection(
    val displayName: String,
    val displayOrder: Int
) {
    POPULAR_TICKET("인기티켓", 1),
    DATE_COURSE("데이트코스", 2),
    RECOMMENDED("이런 티켓은 어때요?", 3),
    REGION("어디로 떠나볼까요?", 4);

    companion object {
        fun getAllSorted(): List<HomeSection> =
            entries.sortedBy { it.displayOrder }
    }
}
