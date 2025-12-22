package com.github.copyinaction.booking.repository

import com.github.copyinaction.booking.domain.SeatLock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface SeatLockRepository : JpaRepository<SeatLock, Long> {

    /**
     * 특정 예매 ID에 해당하는 모든 좌석 잠금을 삭제합니다.
     * @param bookingId 예매 ID
     */
    fun deleteAllByBooking_Id(bookingId: UUID)


    /**
     * 특정 시간 이전에 만료된 모든 좌석 잠금을 삭제합니다. (스케줄러용)
     * @param expiresAt 만료 시각
     * @return 삭제된 row 수
     */
    @Modifying
    @Query("DELETE FROM SeatLock sl WHERE sl.expiresAt < :expiresAt")
    fun deleteAllByExpiresAtBefore(expiresAt: LocalDateTime): Int
}
