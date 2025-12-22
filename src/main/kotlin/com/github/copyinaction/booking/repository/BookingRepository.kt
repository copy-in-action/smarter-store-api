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
     * @param status 예매 상태
     * @return Booking?
     */
    fun findByUser_IdAndSchedule_IdAndStatus(userId: Long, scheduleId: Long, status: BookingStatus): Booking?

    /**
     * 특정 상태와 만료 시간을 기준으로 모든 예매 정보를 찾습니다. (스케줄러용)
     * @param status 예매 상태
     * @param expiresAt 만료 시각
     * @return List<Booking>
     */
    fun findAllByStatusAndExpiresAtBefore(status: BookingStatus, expiresAt: LocalDateTime): List<Booking>
}
