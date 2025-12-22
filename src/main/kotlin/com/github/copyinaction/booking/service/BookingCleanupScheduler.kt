package com.github.copyinaction.booking.service

import com.github.copyinaction.booking.domain.BookingStatus
import com.github.copyinaction.booking.repository.BookingRepository
import com.github.copyinaction.booking.repository.SeatLockRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BookingCleanupScheduler(
    private val bookingRepository: BookingRepository,
    private val seatLockRepository: SeatLockRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRate = 60000) // 1분마다 실행
    @Transactional
    fun cleanupExpiredBookingsAndSeatLocks() {
        val now = LocalDateTime.now()
        log.info("만료된 예매 및 좌석 잠금 정리 스케줄러 실행: {}", now)

        // 1. 만료된 PENDING 상태의 예매 조회
        val expiredBookings = bookingRepository.findAllByStatusAndExpiresAtBefore(BookingStatus.PENDING, now)

        if (expiredBookings.isNotEmpty()) {
            log.info("만료된 PENDING 예매 {}건 발견.", expiredBookings.size)
            expiredBookings.forEach { booking ->
                // 2. Booking 상태를 EXPIRED로 변경
                booking.expire() // 도메인 엔티티의 expire() 메서드 사용
                log.info("예매 ID {} 상태 EXPIRED로 변경.", booking.id)

                // 3. 해당 Booking의 모든 SeatLock 삭제
                seatLockRepository.deleteAllByBooking_Id(booking.id!!)
                log.info("예매 ID {}에 연결된 SeatLock 삭제 완료.", booking.id)
            }
            bookingRepository.saveAll(expiredBookings) // 변경된 상태 저장
            log.info("만료된 PENDING 예매 {}건 처리 완료.", expiredBookings.size)
        } else {
            log.info("만료된 PENDING 예매 없음.")
        }

        // 4. Booking과 연결되지 않은 만료된 SeatLock 삭제 (혹시 모를 잔여 SeatLock 정리)
        val deletedSeatLocksCount = seatLockRepository.deleteAllByExpiresAtBefore(now)
        if (deletedSeatLocksCount > 0) {
            log.info("만료된 SeatLock {}건 추가 삭제 완료.", deletedSeatLocksCount)
        } else {
            log.info("만료된 SeatLock 추가 삭제 없음.")
        }

        log.info("만료된 예매 및 좌석 잠금 정리 스케줄러 종료.")
    }
}
