package com.github.copyinaction.wishlist.controller

import com.github.copyinaction.auth.service.CustomUserDetails
import com.github.copyinaction.common.exception.ErrorResponse
import com.github.copyinaction.wishlist.dto.PagedWishlistResponse
import com.github.copyinaction.wishlist.dto.WishlistStatusResponse
import com.github.copyinaction.wishlist.service.WishlistService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "wishlist-management", description = "찜 관리 API")
@RestController
@RequestMapping("/api/wishlists")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
class WishlistController(
    private val wishlistService: WishlistService
) {

    @Operation(summary = "내 찜 목록 조회", description = "로그인한 사용자의 찜 목록을 페이징 처리하여 조회합니다. (인피니티 스크롤 대응)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공")
    )
    @GetMapping("/me")
    fun getMyWishlists(
        @AuthenticationPrincipal user: CustomUserDetails,
        @ParameterObject @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<PagedWishlistResponse> {
        val response = wishlistService.getMyWishlists(user.id, pageable)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "찜 등록", description = "특정 공연을 찜 목록에 등록합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "등록 성공"),
        ApiResponse(
            responseCode = "404",
            description = "공연을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/{performanceId}")
    fun addWishlist(
        @AuthenticationPrincipal user: CustomUserDetails,
        @Parameter(description = "찜할 공연 ID", required = true) @PathVariable performanceId: Long
    ): ResponseEntity<Unit> {
        wishlistService.addWishlist(user.id, performanceId)
        return ResponseEntity.ok().build()
    }

    @Operation(summary = "찜 해제", description = "특정 공연을 찜 목록에서 삭제합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "해제 성공")
    )
    @DeleteMapping("/{performanceId}")
    fun removeWishlist(
        @AuthenticationPrincipal user: CustomUserDetails,
        @Parameter(description = "해제할 공연 ID", required = true) @PathVariable performanceId: Long
    ): ResponseEntity<Unit> {
        wishlistService.removeWishlist(user.id, performanceId)
        return ResponseEntity.ok().build()
    }
    
    @Operation(summary = "공연 찜 여부 확인", description = "특정 공연의 찜 여부를 확인합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공")
    )
    @GetMapping("/{performanceId}")
    fun checkWishlistStatus(
        @AuthenticationPrincipal user: CustomUserDetails,
        @Parameter(description = "공연 ID", required = true) @PathVariable performanceId: Long,
    ): ResponseEntity<WishlistStatusResponse> {
        val isWishlisted = wishlistService.isWishlisted(user.id, performanceId)
        return ResponseEntity.ok(WishlistStatusResponse(isWishlisted))
    }
}
