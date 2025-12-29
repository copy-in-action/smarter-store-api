package com.github.copyinaction.booking.service

import com.github.copyinaction.auth.domain.Role
import com.github.copyinaction.auth.domain.User
import com.github.copyinaction.auth.repository.UserRepository
import com.github.copyinaction.booking.dto.SeatPositionRequest
import com.github.copyinaction.performance.domain.Performance
import com.github.copyinaction.performance.domain.PerformanceSchedule
import com.github.copyinaction.performance.repository.PerformanceRepository
import com.github.copyinaction.performance.repository.PerformanceScheduleRepository
import com.github.copyinaction.seat.repository.ScheduleSeatStatusRepository
import com.github.copyinaction.venue.domain.Venue
import com.github.copyinaction.venue.repository.VenueRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingServiceTest @Autowired constructor(
    private val bookingService: BookingService,
    private val scheduleSeatStatusRepository: ScheduleSeatStatusRepository,
    private val userRepository: UserRepository,
    private val venueRepository: VenueRepository,
    private val performanceRepository: PerformanceRepository,
    private val performanceScheduleRepository: PerformanceScheduleRepository
) {

    @Test
    @DisplayName("1-based 좌표: 사용자가 (1,1)로 요청하면 그대로 (1,1)이 저장된다")
    fun testOneBasedCoordinateStorage() {
        // Given: 기초 데이터 생성
        val user = userRepository.save(
            User(
                email = "test@test.com",
                username = "tester",
                passwordHash = "encoded_password",
                role = Role.USER
            )
        )

        val venue = venueRepository.save(Venue.create("Test Venue", "Address", "{}", null))
        val performance = performanceRepository.save(
            Performance.create(
                title = "Test Performance",
                description = "Test Description",
                category = "MUSICAL",
                runningTime = 120,
                ageRating = "전체관람가",
                mainImageUrl = null,
                visible = true,
                venue = venue,
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(30)
            )
        )
        val schedule = performanceScheduleRepository.save(
            PerformanceSchedule.create(
                performance = performance,
                showDateTime = LocalDateTime.now().plusDays(1),
                saleStartDateTime = LocalDateTime.now().minusDays(1)
            )
        )

        // When: 사용자가 1-based 좌표 (1, 1)로 예약 요청
        val request = listOf(SeatPositionRequest(row = 1, col = 1))
        bookingService.startBooking(schedule.id, request, user.id)

        // Then: 1-based 좌표 (1, 1)로 그대로 저장되어야 함
        val savedSeat = scheduleSeatStatusRepository.findByScheduleIdAndRowNumAndColNum(schedule.id, 1, 1)
        assertNotNull(savedSeat, "1-based (1,1) 요청은 그대로 (1,1)에 저장되어야 합니다.")
        assertEquals(1, savedSeat!!.rowNum)
        assertEquals(1, savedSeat.colNum)
    }

    @Test
    @DisplayName("1-based 좌표: 사용자가 (3,5)로 요청하면 그대로 (3,5)가 저장된다")
    fun testOneBasedCoordinateStorageMultiple() {
        // Given
        val user = userRepository.save(
            User(
                email = "test2@test.com",
                username = "tester2",
                passwordHash = "encoded_password",
                role = Role.USER
            )
        )

        val venue = venueRepository.save(Venue.create("Test Venue 2", "Address 2", "{}", null))
        val performance = performanceRepository.save(
            Performance.create(
                title = "Test Performance 2",
                description = "Test Description",
                category = "MUSICAL",
                runningTime = 120,
                ageRating = "전체관람가",
                mainImageUrl = null,
                visible = true,
                venue = venue,
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(30)
            )
        )
        val schedule = performanceScheduleRepository.save(
            PerformanceSchedule.create(
                performance = performance,
                showDateTime = LocalDateTime.now().plusDays(1),
                saleStartDateTime = LocalDateTime.now().minusDays(1)
            )
        )

        // When: 1-based 좌표 (3, 5)로 요청
        val request = listOf(SeatPositionRequest(row = 3, col = 5))
        bookingService.startBooking(schedule.id, request, user.id)

        // Then: 그대로 (3, 5)로 저장
        val savedSeat = scheduleSeatStatusRepository.findByScheduleIdAndRowNumAndColNum(schedule.id, 3, 5)
        assertNotNull(savedSeat, "1-based (3,5) 요청은 그대로 (3,5)에 저장되어야 합니다.")
        assertEquals(3, savedSeat!!.rowNum)
        assertEquals(5, savedSeat.colNum)
    }
}
