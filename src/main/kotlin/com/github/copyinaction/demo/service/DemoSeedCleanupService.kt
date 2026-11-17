package com.github.copyinaction.demo.service

import com.github.copyinaction.booking.domain.BookingStatus
import com.github.copyinaction.booking.repository.BookingRepository
import com.github.copyinaction.booking.service.BookingFacade
import com.github.copyinaction.demo.config.ConditionalOnDemoSeed
import com.github.copyinaction.demo.config.DemoSeedProperties
import com.github.copyinaction.performance.repository.PerformanceScheduleRepository
import com.github.copyinaction.seat.repository.ScheduleSeatStatusRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * [Track 3] 봇 이력 정리
 *
 * 1. 봇의 RELEASED/EXPIRED/CANCELLED 예매 삭제 (시뮬레이터가 계속 만들어내는 쓰레기 데이터)
 * 2. 보관 기간(retention-days)이 지난 과거 회차의 봇 예매/좌석 상태 삭제
 * 3. 당일/익일 회차 점유율이 90%를 넘으면 봇의 CONFIRMED 예매를 일부 해제해 "항상 살 수 있는" 회차 유지
 */
@Service
@ConditionalOnDemoSeed
class DemoSeedCleanupService(
    private val userSeedProvider: DemoUserSeedProvider,
    private val bookingRepository: BookingRepository,
    private val scheduleSeatStatusRepository: ScheduleSeatStatusRepository,
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val bookingFacade: BookingFacade,
    private val seatSupport: DemoSeatSupport,
    private val properties: DemoSeedProperties,
    transactionManager: PlatformTransactionManager
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val txTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    companion object {
        /** 이 점유율을 넘으면 봇 예매를 해제하기 시작한다. */
        const val OCCUPANCY_UPPER_LIMIT = 0.90

        /** 해제를 시작한 뒤 목표로 하는 점유율 */
        const val OCCUPANCY_RELEASE_TARGET = 0.85
    }

    /**
     * 전체 정리 실행
     */
    fun cleanup(): CleanupResult {
        val botIds = userSeedProvider.findBotUserIds()
        if (botIds.isEmpty()) {
            log.info("[DemoSeed] 봇 유저가 없어 정리를 건너뜁니다.")
            return CleanupResult(0, 0, 0)
        }

        val staleBookings = deleteInactiveBotBookings(botIds)
        val pastBookings = deletePastBotData(botIds)
        val released = releaseOverbookedSchedules(botIds)

        log.info(
            "[DemoSeed] 봇 이력 정리 완료: 비활성 예매 {}건, 과거 회차 예매 {}건, 점유율 조정 해제 {}건",
            staleBookings, pastBookings, released
        )
        return CleanupResult(staleBookings, pastBookings, released)
    }

    /**
     * 1) 봇의 RELEASED / EXPIRED / CANCELLED 예매 삭제
     * 좌석 상태는 해제 시점에 이미 삭제되므로 예매 좌석(booking_seat) -> 예매(booking) 순으로 지운다.
     */
    private fun deleteInactiveBotBookings(botIds: List<Long>): Int {
        val statuses = listOf(BookingStatus.RELEASED, BookingStatus.EXPIRED, BookingStatus.CANCELLED)
        return txTemplate.execute {
            bookingRepository.deleteSeatsByUserIdsAndStatuses(botIds, statuses)
            bookingRepository.deleteByUserIdsAndStatuses(botIds, statuses)
        } ?: 0
    }

    /**
     * 2) 보관 기간이 지난 과거 회차의 봇 좌석 상태/예매 삭제
     */
    private fun deletePastBotData(botIds: List<Long>): Int {
        val cutoff = LocalDateTime.now().minusDays(properties.retentionDays.toLong())
        return txTemplate.execute {
            scheduleSeatStatusRepository.deleteAllByHeldByInAndShowDateTimeBefore(botIds, cutoff)
            bookingRepository.deleteSeatsByUserIdsAndShowDateTimeBefore(botIds, cutoff)
            bookingRepository.deleteByUserIdsAndShowDateTimeBefore(botIds, cutoff)
        } ?: 0
    }

    /**
     * 3) 당일/익일 회차의 점유율이 90%를 넘으면 봇 CONFIRMED 예매를 해제해 85% 수준으로 낮춘다.
     * BookingFacade.cancelBooking 을 사용하므로 좌석 상태 삭제와 SSE RELEASED 전파가 함께 처리된다.
     */
    private fun releaseOverbookedSchedules(botIds: List<Long>): Int {
        val now = LocalDateTime.now()
        val endOfTomorrow = LocalDate.now().plusDays(1).atTime(LocalTime.MAX)
        val scheduleIds = performanceScheduleRepository.findScheduleIdsBetween(now, endOfTomorrow)

        var released = 0
        for (scheduleId in scheduleIds) {
            try {
                var ratio = seatSupport.occupancyRatio(scheduleId)
                if (ratio <= OCCUPANCY_UPPER_LIMIT) continue

                val botBookings = txTemplate.execute {
                    bookingRepository.findAllBySchedule_IdAndSiteUser_IdInAndBookingStatus(
                        scheduleId, botIds, BookingStatus.CONFIRMED
                    ).map { Triple(it.id!!, it.siteUser.id, it.bookingSeats.size) }
                } ?: emptyList()

                // 봇 예매가 없으면(실사용자 예매로 가득 찬 회차) 건드릴 수 있는 것이 없다.
                if (botBookings.isEmpty()) continue

                for ((bookingId, userId, _) in botBookings.shuffled()) {
                    if (ratio <= OCCUPANCY_RELEASE_TARGET) break
                    bookingFacade.cancelBooking(bookingId, userId, "데모 점유율 조정")
                    released++
                    ratio = seatSupport.occupancyRatio(scheduleId)
                }
            } catch (e: Exception) {
                log.warn("[DemoSeed] 회차 {} 점유율 조정 실패: {}", scheduleId, e.message, e)
            }
        }
        return released
    }
}

/**
 * 정리 결과 요약
 */
data class CleanupResult(
    val inactiveBotBookingsDeleted: Int,
    val pastBotBookingsDeleted: Int,
    val overbookedBookingsReleased: Int
)
