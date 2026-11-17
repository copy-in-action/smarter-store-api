package com.github.copyinaction.demo.service

import com.github.copyinaction.booking.dto.SeatPositionRequest
import com.github.copyinaction.demo.config.ConditionalOnDemoSeed
import com.github.copyinaction.performance.repository.PerformanceScheduleRepository
import com.github.copyinaction.performance.repository.TicketOptionRepository
import com.github.copyinaction.seat.repository.ScheduleSeatStatusRepository
import com.github.copyinaction.venue.util.SeatingChartParser
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 시뮬레이터/정리 배치가 공통으로 사용하는 좌석 조회 도구
 * - 읽기 전용 트랜잭션 안에서 엔티티 지연 로딩(performance.venue 등)을 안전하게 수행하기 위해 별도 빈으로 분리
 */
@Service
@ConditionalOnDemoSeed
class DemoSeatSupport(
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val scheduleSeatStatusRepository: ScheduleSeatStatusRepository,
    private val ticketOptionRepository: TicketOptionRepository,
    private val seatingChartParser: SeatingChartParser
) {

    /**
     * 회차에서 비어 있는 좌석 연석 [size]석을 골라 예매 요청 좌표(1-based)로 반환한다.
     * 비활성화 좌석/점유 중 좌석/통로 건너편 좌석은 제외된다. 후보가 없으면 null.
     */
    @Transactional(readOnly = true)
    fun pickFreeSeats(scheduleId: Long, size: Int): List<SeatPositionRequest>? {
        val schedule = performanceScheduleRepository.findByIdOrNull(scheduleId) ?: return null
        val venue = schedule.performance.venue ?: return null
        val fullLayout = seatingChartParser.parseLayout(venue.seatingChart) ?: return null
        val availableGrades = schedule.ticketOptions.map { it.seatGrade }.toSet()
        val layout = fullLayout.copy(rowGrades = fullLayout.rowGrades.filterValues { it in availableGrades })

        // 만료된 PENDING 은 점유가 아닌 것으로 취급 (startBooking 시 occupySeats 가 정리함)
        val occupied = scheduleSeatStatusRepository.findByScheduleId(scheduleId)
            .filter { !it.isExpired() }
            .map { Pair(it.rowNum, it.colNum) }

        val seats = DemoSeatAllocator(layout, occupied).allocateBestEffort(size) ?: return null
        return seats.map { SeatPositionRequest(row = it.row, col = it.col) }
    }

    /**
     * 회차의 현재 점유율(0.0~1.0) = (PENDING + RESERVED 좌석 수) / 등급별 총 좌석 수 합계
     */
    @Transactional(readOnly = true)
    fun occupancyRatio(scheduleId: Long): Double {
        val total = ticketOptionRepository.findByPerformanceScheduleId(scheduleId).sumOf { it.totalQuantity }
        if (total <= 0) return 0.0
        val occupied = scheduleSeatStatusRepository.countByScheduleId(scheduleId)
        return occupied.toDouble() / total
    }
}
