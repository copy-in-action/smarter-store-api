package com.github.copyinaction.booking.repository

import com.github.copyinaction.booking.domain.Booking
import com.github.copyinaction.booking.domain.BookingStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface BookingRepository : JpaRepository<Booking, UUID> {

    /**
     * 사용자와 공연 회차, 예매 상태로 예매 정보를 찾습니다.
     * @param userId 사용자 ID
     * @param scheduleId 공연 회차 ID
     * @param bookingStatus 예매 상태
     * @return Booking?
     */
    fun findBySiteUser_IdAndSchedule_IdAndBookingStatus(userId: Long, scheduleId: Long, bookingStatus: BookingStatus): Booking?

    /**
     * 특정 상태와 만료 시간을 기준으로 모든 예매 정보를 찾습니다. (스케줄러용)
     * @param bookingStatus 예매 상태
     * @param expiresAt 만료 시각
     * @return List<Booking>
     */
    fun findAllByBookingStatusAndExpiresAtBefore(bookingStatus: BookingStatus, expiresAt: LocalDateTime): List<Booking>

    /**
     * 특정 회차에 특정 상태 목록에 포함되는 예매가 존재하는지 확인합니다.
     */
    fun existsBySchedule_IdAndBookingStatusIn(scheduleId: Long, bookingStatuses: Collection<BookingStatus>): Boolean
}
