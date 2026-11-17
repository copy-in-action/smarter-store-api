package com.github.copyinaction.demo.service

import com.github.copyinaction.auth.repository.UserRepository
import com.github.copyinaction.booking.repository.BookingRepository
import com.github.copyinaction.demo.config.ConditionalOnDemoSeed
import com.github.copyinaction.demo.config.DemoSeedProperties
import com.github.copyinaction.performance.repository.PerformanceScheduleRepository
import com.github.copyinaction.seat.domain.ScheduleSeatStatus
import com.github.copyinaction.seat.repository.ScheduleSeatStatusRepository
import com.github.copyinaction.venue.util.SeatingChartParser
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * [Track 1-2] 신규 회차 초기 좌석 시딩
 *
 * 봇 유저 명의의 `CONFIRMED` 예매 + `RESERVED` 좌석 상태를 직접 저장해서
 * 데모 서비스가 "이미 많이 팔린 공연"처럼 보이게 한다. (Payment는 생성하지 않음)
 *
 * 회차 1건당 1개의 트랜잭션으로 처리하며, 실패해도 다른 회차에 영향을 주지 않는다.
 */
@Service
@ConditionalOnDemoSeed
class DemoBookingSeedService(
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val scheduleSeatStatusRepository: ScheduleSeatStatusRepository,
    private val bookingRepository: BookingRepository,
    private val userRepository: UserRepository,
    private val bookingFactory: DemoBookingFactory,
    private val seatingChartParser: SeatingChartParser,
    private val properties: DemoSeedProperties,
    transactionManager: PlatformTransactionManager
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val txTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    /**
     * 주어진 회차들에 초기 좌석을 시딩한다.
     *
     * @param scheduleIds 시딩 대상 회차 ID
     * @param botUserIds 예매자로 사용할 봇 유저 ID
     * @return 시딩된 총 좌석 수
     */
    fun seedSchedules(scheduleIds: Collection<Long>, botUserIds: List<Long>): Int {
        if (scheduleIds.isEmpty()) return 0
        if (botUserIds.isEmpty()) {
            log.warn("[DemoSeed] 봇 유저가 없어 초기 좌석 시딩을 건너뜁니다.")
            return 0
        }

        val usedBookingNumbers = bookingFactory.loadUsedBookingNumbers()
        var totalSeats = 0
        var failed = 0
        for (scheduleId in scheduleIds) {
            try {
                totalSeats += txTemplate.execute { seedSchedule(scheduleId, botUserIds, usedBookingNumbers) } ?: 0
            } catch (e: Exception) {
                failed++
                log.warn("[DemoSeed] 회차 {} 초기 좌석 시딩 실패: {}", scheduleId, e.message, e)
            }
        }
        log.info(
            "[DemoSeed] 초기 좌석 시딩 완료: 회차 {}건(실패 {}건), 좌석 {}석",
            scheduleIds.size, failed, totalSeats
        )
        return totalSeats
    }

    /**
     * 회차 1건 시딩 (호출자가 트랜잭션 경계를 제공해야 한다)
     *
     * @return 이번에 시딩된 좌석 수
     */
    fun seedSchedule(scheduleId: Long, botUserIds: List<Long>, usedBookingNumbers: MutableSet<String>): Int {
        val schedule = performanceScheduleRepository.findByIdOrNull(scheduleId) ?: return 0
        val venue = schedule.performance.venue ?: return 0
        val fullLayout = seatingChartParser.parseLayout(venue.seatingChart) ?: return 0

        // 가격(TicketOption)이 없는 등급의 행은 예매할 수 없으므로 풀에서 제외
        val priceByGrade = schedule.ticketOptions.associate { it.seatGrade to it.price }
        val layout = fullLayout.copy(rowGrades = fullLayout.rowGrades.filterValues { it in priceByGrade })

        val poolSize = layout.availableSeats().size
        if (poolSize == 0) return 0

        // 이미 점유/예약된 좌석은 제외 (재실행/수동 호출 시 충돌 방지)
        val occupied = scheduleSeatStatusRepository.findByScheduleId(scheduleId)
            .map { Pair(it.rowNum, it.colNum) }

        val ratio = DemoOccupancyPreset.targetRatio(schedule.showDateTime, LocalDate.now())
        // 대형 공연장에서 행 폭발을 막기 위해 회차당 초기 시딩 좌석 수 상한 적용
        val targetTotal = minOf((poolSize * ratio).roundToInt(), properties.maxSeatsPerSchedule)
        val targetNew = targetTotal - occupied.size
        if (targetNew <= 0) return 0

        val bots = userRepository.findAllById(botUserIds).toList()
        if (bots.isEmpty()) return 0

        val allocator = DemoSeatAllocator(layout, occupied)
        val bookings = mutableListOf<com.github.copyinaction.booking.domain.Booking>()
        val seatStatuses = mutableListOf<ScheduleSeatStatus>()
        var seeded = 0

        while (seeded < targetNew) {
            val remaining = targetNew - seeded
            val groupSize = minOf(DemoOccupancyPreset.pickGroupSize(), remaining, 4)
            val group = allocator.allocateBestEffort(groupSize) ?: break

            val grade = group.first().grade
            val price = priceByGrade.getValue(grade)
            val bot = bots[Random.nextInt(bots.size)]

            val booking = bookingFactory.create(bot, schedule, usedBookingNumbers)
            booking.addSeats(group.map { Pair(it.row, it.col) }, grade, price)
            booking.confirm()
            bookings.add(booking)

            group.forEach { seat ->
                seatStatuses.add(ScheduleSeatStatus.reserve(schedule, seat.row, seat.col, seat.grade, bot.id))
            }
            seeded += group.size
        }

        bookingRepository.saveAll(bookings)
        scheduleSeatStatusRepository.saveAll(seatStatuses)

        log.debug("[DemoSeed] 회차 {} 초기 좌석 {}석 시딩 (목표 점유율 {}%)", scheduleId, seeded, (ratio * 100).roundToInt())
        return seeded
    }
}
