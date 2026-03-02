package com.github.copyinaction.wishlist.service

import com.github.copyinaction.auth.repository.UserRepository
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.performance.repository.PerformanceRepository
import com.github.copyinaction.performance.repository.TicketOptionRepository
import com.github.copyinaction.wishlist.domain.Wishlist
import com.github.copyinaction.wishlist.dto.PagedWishlistResponse
import com.github.copyinaction.wishlist.dto.WishlistMetaResponse
import com.github.copyinaction.wishlist.dto.WishlistResponse
import com.github.copyinaction.wishlist.repository.WishlistRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.text.DecimalFormat

@Service
@Transactional(readOnly = true)
class WishlistService(
    private val wishlistRepository: WishlistRepository,
    private val userRepository: UserRepository,
    private val performanceRepository: PerformanceRepository,
    private val ticketOptionRepository: TicketOptionRepository
) {
    private val decimalFormat = DecimalFormat("#,###")

    /**
     * 찜 등록
     */
    @Transactional
    fun addWishlist(userId: Long, performanceId: Long) {
        if (wishlistRepository.existsBySiteUser_IdAndPerformance_Id(userId, performanceId)) {
            return
        }

        val user = userRepository.findByIdOrNull(userId) ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        val performance = performanceRepository.findByIdOrNull(performanceId)
            ?: throw CustomException(ErrorCode.PERFORMANCE_NOT_FOUND)

        val wishlist = Wishlist.create(user, performance)
        wishlistRepository.save(wishlist)
    }

    /**
     * 찜 해제
     */
    @Transactional
    fun removeWishlist(userId: Long, performanceId: Long) {
        wishlistRepository.deleteBySiteUser_IdAndPerformance_Id(userId, performanceId)
    }

    /**
     * 내 찜 목록 조회 (인피니티 스크롤 대응)
     */
    fun getMyWishlists(userId: Long, pageable: Pageable): PagedWishlistResponse {
        // "string"과 같이 유효하지 않은 정렬 속성이 들어오는 경우를 대비하여 필터링
        val validatedPageable = if (pageable.sort.any { it.property == "string" }) {
            PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        } else {
            pageable
        }

        val wishlistPage = wishlistRepository.findBySiteUser_Id(userId, validatedPageable)
        
        val wishlistResponses = wishlistPage.content.map { wishlist ->
            val performance = wishlist.performance
            val minPrice = ticketOptionRepository.findMinPriceByPerformanceId(performance.id) ?: 0
            
            WishlistResponse(
                performanceId = performance.id,
                title = performance.title,
                mainImageUrl = performance.mainImageUrl,
                location = performance.venue?.name ?: "장소 미정",
                priceInfo = "${decimalFormat.format(minPrice)}원~"
            )
        }

        val meta = WishlistMetaResponse(
            totalCount = wishlistPage.totalElements,
            currentPage = wishlistPage.number,
            pageSize = wishlistPage.size,
            totalPages = wishlistPage.totalPages,
            hasNextPage = wishlistPage.hasNext(),
            nextCursor = if (wishlistPage.hasNext()) (wishlistPage.number + 1) * wishlistPage.size else null
        )

        return PagedWishlistResponse(
            data = wishlistResponses,
            meta = meta
        )
    }
    
    /**
     * 찜 여부 확인
     */
    fun isWishlisted(userId: Long, performanceId: Long): Boolean {
        return wishlistRepository.existsBySiteUser_IdAndPerformance_Id(userId, performanceId)
    }
}
