package com.github.copyinaction.reservation.scheduler

import com.github.copyinaction.reservation.repository.ScheduleSeatRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class SeatHoldExpirationScheduler(
    private val scheduleSeatRepository: ScheduleSeatRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 만료된 좌석 점유를 해제합니다.
     * 1분마다 실행됩니다.
     */
    @Scheduled(fixedRate = 60000) // 60초마다 실행
    @Transactional
    fun releaseExpiredHolds() {
        val now = LocalDateTime.now()
        val expiredSeats = scheduleSeatRepository.findExpiredHolds(now)

        if (expiredSeats.isNotEmpty()) {
            expiredSeats.forEach { seat ->
                seat.release()
            }

            logger.info("Released ${expiredSeats.size} expired seat holds at $now")
        }
    }
}
