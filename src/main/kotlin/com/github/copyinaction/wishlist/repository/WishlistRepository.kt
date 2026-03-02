package com.github.copyinaction.wishlist.repository

import com.github.copyinaction.wishlist.domain.Wishlist
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface WishlistRepository : JpaRepository<Wishlist, UUID> {
    /**
     * 특정 사용자가 특정 공연을 찜했는지 여부를 확인합니다.
     */
    fun existsBySiteUser_IdAndPerformance_Id(userId: Long, performanceId: Long): Boolean

    /**
     * 특정 사용자의 특정 공연 찜 정보를 삭제합니다.
     */
    fun deleteBySiteUser_IdAndPerformance_Id(userId: Long, performanceId: Long)

    /**
     * 특정 사용자의 찜 목록을 페이징 처리하여 조회합니다.
     * JOIN FETCH를 사용하여 N+1 문제를 방지하며, 페이징을 위해 countQuery를 별도로 작성합니다.
     */
    @Query(
        value = "SELECT w FROM Wishlist w JOIN FETCH w.performance p WHERE w.siteUser.id = :userId",
        countQuery = "SELECT count(w) FROM Wishlist w WHERE w.siteUser.id = :userId"
    )
    fun findBySiteUser_Id(userId: Long, pageable: Pageable): Page<Wishlist>
}
