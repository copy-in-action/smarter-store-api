package com.github.copyinaction.wishlist.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "페이징 처리된 찜 목록 응답")
data class PagedWishlistResponse(
    @Schema(description = "찜 데이터 리스트")
    val data: List<WishlistResponse>,

    @Schema(description = "페이징 메타데이터")
    val meta: WishlistMetaResponse
)

@Schema(description = "페이징 메타데이터")
data class WishlistMetaResponse(
    @Schema(description = "전체 아이템 수")
    val totalCount: Long,

    @Schema(description = "현재 페이지 (0-based)")
    val currentPage: Int,

    @Schema(description = "한 페이지당 아이템 수")
    val pageSize: Int,

    @Schema(description = "전체 페이지 수")
    val totalPages: Int,

    @Schema(description = "다음 페이지 존재 여부")
    val hasNextPage: Boolean,

    @Schema(description = "Cursor 기반일 경우 다음 시작점 (선택 사항)")
    val nextCursor: Int? = null
)
