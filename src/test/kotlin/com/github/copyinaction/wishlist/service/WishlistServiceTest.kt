package com.github.copyinaction.wishlist.service

import com.github.copyinaction.auth.domain.User
import com.github.copyinaction.auth.repository.UserRepository
import com.github.copyinaction.performance.domain.Performance
import com.github.copyinaction.performance.repository.PerformanceRepository
import com.github.copyinaction.performance.repository.TicketOptionRepository
import com.github.copyinaction.wishlist.repository.WishlistRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

@DisplayName("WishlistService 테스트")
class WishlistServiceTest {

    private lateinit var wishlistService: WishlistService
    private val wishlistRepository: WishlistRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val performanceRepository: PerformanceRepository = mockk()
    private val ticketOptionRepository: TicketOptionRepository = mockk()

    @BeforeEach
    fun setUp() {
        wishlistService = WishlistService(
            wishlistRepository,
            userRepository,
            performanceRepository,
            ticketOptionRepository
        )
    }

    @Test
    @DisplayName("찜 여부 확인 - 찜 등록된 경우 true를 반환한다")
    fun `isWishlisted returns true when wishlisted`() {
        // Given
        val userId = 1L
        val performanceId = 100L

        every { wishlistRepository.existsBySiteUser_IdAndPerformance_Id(userId, performanceId) } returns true

        // When
        val result = wishlistService.isWishlisted(userId, performanceId)

        // Then
        assertTrue(result)
    }

    @Test
    @DisplayName("찜 여부 확인 - 찜 등록되지 않은 경우 false를 반환한다")
    fun `isWishlisted returns false when not wishlisted`() {
        // Given
        val userId = 1L
        val performanceId = 100L

        every { wishlistRepository.existsBySiteUser_IdAndPerformance_Id(userId, performanceId) } returns false

        // When
        val result = wishlistService.isWishlisted(userId, performanceId)

        // Then
        assertFalse(result)
    }
}
