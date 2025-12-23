package com.github.copyinaction.booking.service

import com.github.copyinaction.booking.domain.BookingStatus
import com.github.copyinaction.booking.repository.BookingRepository
import com.github.copyinaction.seat.domain.SeatStatus
import com.github.copyinaction.seat.dto.SeatPosition
import com.github.copyinaction.seat.repository.ScheduleSeatStatusRepository
import com.github.copyinaction.seat.service.SseService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BookingCleanupScheduler(
    private val bookingRepository: BookingRepository,
    private val scheduleSeatStatusRepository: ScheduleSeatStatusRepository,
    private val sseService: SseService
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
            log.info("만료된 PENDING 예매 {}건 발견.", expiredBookings.size)
            expiredBookings.forEach { booking ->
                val scheduleId = booking.schedule.id
                val seatPositions = booking.bookingSeats.map {
                    SeatPosition(it.rowName.toIntOrNull() ?: 0, it.seatNumber)
                }

                // 2. Booking 상태를 EXPIRED로 변경
                booking.expire()
                log.info("예매 ID {} 상태 EXPIRED로 변경.", booking.id)

                // 3. 해당 유저의 좌석 점유 해제
                scheduleSeatStatusRepository.deleteByScheduleIdAndHeldByAndSeatStatus(
                    scheduleId, booking.user.id, SeatStatus.PENDING
                )

                // 4. SSE RELEASED 이벤트 발행
                if (seatPositions.isNotEmpty()) {
                    sseService.sendReleased(scheduleId, seatPositions)
                }
            }
            bookingRepository.saveAll(expiredBookings)
            log.info("만료된 PENDING 예매 {}건 처리 완료.", expiredBookings.size)
        }

        // 5. 만료된 좌석 점유 정리 (Booking 없이 남아있는 만료된 점유)
        val deletedCount = scheduleSeatStatusRepository.deleteExpiredHolds(now)
        if (deletedCount > 0) {
            log.info("만료된 좌석 점유 {}건 추가 삭제 완료.", deletedCount)
        }
    }
}
