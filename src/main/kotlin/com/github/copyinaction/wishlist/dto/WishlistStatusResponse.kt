package com.github.copyinaction.wishlist.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "찜 상태 응답 DTO")
data class WishlistStatusResponse(
    @Schema(description = "찜 여부", example = "true")
    val isWishlisted: Boolean
)
