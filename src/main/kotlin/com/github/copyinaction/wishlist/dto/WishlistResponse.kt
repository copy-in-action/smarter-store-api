package com.github.copyinaction.wishlist.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "찜 목록 개별 공연 정보")
data class WishlistResponse(
    @Schema(description = "공연 ID")
    val performanceId: Long,

    @Schema(description = "공연 제목")
    val title: String,

    @Schema(description = "공연 메인 이미지 URL")
    val mainImageUrl: String?,

    @Schema(description = "공연장 명칭")
    val location: String,

    @Schema(description = "가격 정보 (예: '70,000원~')")
    val priceInfo: String
) {
    companion object {
        fun from(wishlist: com.github.copyinaction.wishlist.domain.Wishlist, minPrice: Int?): WishlistResponse {
            val performance = wishlist.performance
            return WishlistResponse(
                performanceId = performance.id,
                title = performance.title,
                mainImageUrl = performance.mainImageUrl,
                location = performance.venue?.name ?: "미정",
                priceInfo = minPrice?.let { "${"%,d".format(it)}원~" } ?: "가격 정보 없음"
            )
        }
    }
}
