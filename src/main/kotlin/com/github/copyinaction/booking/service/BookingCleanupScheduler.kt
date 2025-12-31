package com.github.copyinaction.booking.service

import com.github.copyinaction.booking.domain.BookingStatus
import com.github.copyinaction.booking.repository.BookingRepository
import com.github.copyinaction.seat.dto.SeatPosition
import com.github.copyinaction.seat.repository.ScheduleSeatStatusRepository
import com.github.copyinaction.seat.service.SeatOccupationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BookingCleanupScheduler(
    private val bookingRepository: BookingRepository,
    private val scheduleSeatStatusRepository: ScheduleSeatStatusRepository,
    private val seatOccupationService: SeatOccupationService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRate = 60000) // 1분마다 실행
    @Transactional
    fun cleanupExpiredBookingsAndSeatLocks() {
        val now = LocalDateTime.now()
        log.debug("만료된 예매 및 좌석 점유 정리 스케줄러 실행: {}", now)

        // 1. 만료된 PENDING 상태의 예매 조회
        val expiredBookings = bookingRepository.findAllByStatusAndExpiresAtBefore(BookingStatus.PENDING, now)

        if (expiredBookings.isNotEmpty()) {
            expiredBookings.forEach { booking ->
                val scheduleId = booking.schedule.id
                val seatPositions = booking.bookingSeats.map {
                    SeatPosition(it.rowName.toIntOrNull() ?: 0, it.seatNumber)
                }
                booking.expire()
                seatOccupationService.releaseUserPendingSeats(scheduleId, booking.user.id, seatPositions)
            }
            bookingRepository.saveAll(expiredBookings)
            log.info("만료된 PENDING 예매 {}건 처리 완료", expiredBookings.size)
        }

        // 4. 만료된 좌석 점유 정리 (Booking 없이 남아있는 만료된 점유)
        val deletedCount = scheduleSeatStatusRepository.deleteExpiredHolds(now)
        if (deletedCount > 0) {
            log.info("만료된 좌석 점유 {}건 추가 삭제 완료.", deletedCount)
        }
    }
}
