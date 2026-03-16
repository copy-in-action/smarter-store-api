package com.github.copyinaction.booking.domain

import com.github.copyinaction.auth.domain.User
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.performance.domain.PerformanceSchedule
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class BookingTest {

    private val user: User = mockk()
    private val schedule: PerformanceSchedule = mockk()

    @Test
    @DisplayName("예매 생성 시 초기 상태는 PENDING이어야 한다")
    fun createBookingTest() {
        val booking = Booking.create(user, schedule)
        assertEquals(BookingStatus.PENDING, booking.bookingStatus)
        assertNotNull(booking.expiresAt)
    }

    @Test
    @DisplayName("만료된 예매를 확정하려고 하면 예외가 발생한다")
    fun confirmExpiredBookingTest() {
        val booking = Booking.create(user, schedule)
        // 강제로 만료 시간을 과거로 설정 (Reflection 대신 접근 가능한 필드가 있다면 사용)
        // 현재는 생성된지 2분 후 만료이므로, 테스트에서는 만료 상태를 임의로 변경하거나 mock 필요
        // 도메인 메서드 중 expire()가 있으므로 이를 활용
        booking.expire()
        
        val exception = assertThrows<CustomException> {
            booking.confirm()
        }
        assertEquals(ErrorCode.BOOKING_EXPIRED, exception.errorCode)
    }

    @Test
    @DisplayName("PENDING 상태가 아닌 예매를 확정하려고 하면 예외가 발생한다")
    fun confirmInvalidStatusBookingTest() {
        val booking = Booking.create(user, schedule)
        booking.confirm() // CONFIRMED 상태로 변경
        
        val exception = assertThrows<CustomException> {
            booking.confirm()
        }
        assertEquals(ErrorCode.BOOKING_INVALID_STATUS, exception.errorCode)
    }

    @Test
    @DisplayName("CONFIRMED 상태의 예매는 취소(CANCELLED)할 수 있다")
    fun cancelConfirmedBookingTest() {
        val booking = Booking.create(user, schedule)
        booking.confirm()
        
        booking.cancel()
        assertEquals(BookingStatus.CANCELLED, booking.bookingStatus)
    }

    @Test
    @DisplayName("PENDING 상태의 예매는 해제(RELEASED)할 수 있다")
    fun releasePendingBookingTest() {
        val booking = Booking.create(user, schedule)
        
        booking.release()
        assertEquals(BookingStatus.RELEASED, booking.bookingStatus)
    }
}
