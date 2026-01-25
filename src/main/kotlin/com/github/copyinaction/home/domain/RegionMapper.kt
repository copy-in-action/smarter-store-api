package com.github.copyinaction.home.domain

/**
 * 공연장 주소를 기반으로 지역 태그를 자동 매핑하는 유틸리티
 */
object RegionMapper {

    private val regionKeywords: Map<HomeSectionTag, List<String>> = mapOf(
        HomeSectionTag.REGION_SEOUL to listOf("서울"),
        HomeSectionTag.REGION_GYEONGGI to listOf(
            "경기", "인천",
            "수원", "성남", "고양", "용인", "안양", "부천",
            "의정부", "남양주", "화성", "평택", "안산", "시흥"
        ),
        HomeSectionTag.REGION_BUSAN to listOf("부산"),
        HomeSectionTag.REGION_DAEGU to listOf("대구"),
        HomeSectionTag.REGION_DAEJEON to listOf("대전", "세종", "충북", "충남", "천안", "청주")
    )

    /**
     * 주소에서 지역 태그를 추출
     * @param address 공연장 주소
     * @return 매핑된 지역 태그, 매핑 불가 시 REGION_NATIONWIDE
     */
    fun mapFromAddress(address: String?): HomeSectionTag {
        if (address.isNullOrBlank()) {
            return HomeSectionTag.REGION_NATIONWIDE
        }

        return regionKeywords.entries
            .find { (_, keywords) -> keywords.any { address.contains(it) } }
            ?.key
            ?: HomeSectionTag.REGION_NATIONWIDE
    }

    /**
     * 해당 태그가 지역 태그인지 확인
     */
    fun isRegionTag(tag: HomeSectionTag): Boolean {
        return tag.section == HomeSection.REGION
    }
}
