package com.github.copyinaction.performance.domain

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 대한민국 17개 광역자치단체 (행정구역)
 */
@Schema(enumAsRef = true, description = "대한민국 17개 광역자치단체 (행정구역)")
enum class Region(
    val displayName: String,
    val keywords: List<String>
) {
    @Schema(description = "서울")
    SEOUL("서울", listOf("서울")),

    @Schema(description = "인천")
    INCHEON("인천", listOf("인천")),

    @Schema(description = "대전")
    DAEJEON("대전", listOf("대전")),

    @Schema(description = "대구")
    DAEGU("대구", listOf("대구")),

    @Schema(description = "광주")
    GWANGJU("광주", listOf("광주")),

    @Schema(description = "울산")
    ULSAN("울산", listOf("울산")),

    @Schema(description = "부산")
    BUSAN("부산", listOf("부산")),

    @Schema(description = "세종")
    SEJONG("세종", listOf("세종")),

    @Schema(description = "경기")
    GYEONGGI("경기", listOf("경기")),

    @Schema(description = "강원")
    GANGWON("강원", listOf("강원")),

    @Schema(description = "충북")
    CHUNGBUK("충북", listOf("충북", "충청북도")),

    @Schema(description = "충남")
    CHUNGNAM("충남", listOf("충남", "충청남도")),

    @Schema(description = "전북")
    JEONBUK("전북", listOf("전북", "전라북도")),

    @Schema(description = "전남")
    JEONNAM("전남", listOf("전남", "전라남도")),

    @Schema(description = "경북")
    GYEONGBUK("경북", listOf("경북", "경상북도")),

    @Schema(description = "경남")
    GYEONGNAM("경남", listOf("경남", "경상남도")),

    @Schema(description = "제주")
    JEJU("제주", listOf("제주"));

    companion object {
        fun fromDisplayName(name: String): Region? {
            return entries.find { it.displayName == name }
        }

        /**
         * 주소 문자열에서 매칭되는 지역을 찾음
         */
        fun findFromAddress(address: String?): Region? {
            if (address.isNullOrBlank()) return null
            return entries.find { region ->
                region.keywords.any { keyword -> address.contains(keyword) }
            }
        }
    }
}
