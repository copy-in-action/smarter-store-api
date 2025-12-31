package com.github.copyinaction.performance.service

import com.github.copyinaction.performance.repository.PerformanceScheduleRepository
import com.github.copyinaction.performance.repository.TicketOptionRepository
import com.github.copyinaction.venue.domain.SeatGrade
import com.github.copyinaction.venue.util.SeatingChartParser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * TicketOption 동기화 서비스
 * - Venue 좌석배치도 변경 시 관련 TicketOption의 totalQuantity를 동기화
 * - Aggregate 경계를 존중하여 Performance 도메인 내에서 TicketOption 관리
 */
@Service
class TicketOptionSyncService(
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val ticketOptionRepository: TicketOptionRepository,
    private val seatingChartParser: SeatingChartParser
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 좌석배치도 변경 시 관련 스케줄의 TicketOption.totalQuantity 동기화
     * - 미래 스케줄(공연 시작 전)만 대상
     *
     * @param venueId 공연장 ID
     * @param seatingChartJson 변경된 좌석배치도 JSON
     * @return 업데이트된 TicketOption 수
     */
    @Transactional
    fun syncTotalQuantityByVenue(venueId: Long, seatingChartJson: String): Int {
        val now = LocalDateTime.now()
        val futureSchedules = performanceScheduleRepository.findFutureSchedulesByVenueId(venueId, now)

        if (futureSchedules.isEmpty()) {
            log.info("Venue {} 좌석배치도 변경: 동기화할 미래 스케줄 없음", venueId)
            return 0
        }

        val seatsByGrade: Map<SeatGrade, Int> = seatingChartParser.countSeatsByGrade(seatingChartJson)

        var updatedCount = 0
        for (schedule in futureSchedules) {
            val ticketOptions = ticketOptionRepository.findByPerformanceScheduleId(schedule.id)
            for (option in ticketOptions) {
                val newTotalQuantity = seatsByGrade[option.seatGrade] ?: 0
                if (option.totalQuantity != newTotalQuantity) {
                    option.totalQuantity = newTotalQuantity
                    updatedCount++
                }
            }
        }

        log.info("Venue {} 좌석배치도 변경: {} 스케줄, {} TicketOption 동기화 완료",
            venueId, futureSchedules.size, updatedCount)

        return updatedCount
    }
}
