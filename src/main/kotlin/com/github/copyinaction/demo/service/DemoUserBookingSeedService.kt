package com.github.copyinaction.demo.service

import com.github.copyinaction.auth.domain.User
import com.github.copyinaction.booking.domain.Booking
import com.github.copyinaction.booking.domain.BookingStatus
import com.github.copyinaction.booking.repository.BookingRepository
import com.github.copyinaction.coupon.repository.CouponUsageRepository
import com.github.copyinaction.demo.config.ConditionalOnDemoSeed
import com.github.copyinaction.payment.domain.Payment
import com.github.copyinaction.payment.domain.PaymentItem
import com.github.copyinaction.payment.domain.PaymentMethod
import com.github.copyinaction.payment.repository.PaymentRepository
import com.github.copyinaction.performance.domain.PerformanceSchedule
import com.github.copyinaction.performance.repository.PerformanceScheduleRepository
import com.github.copyinaction.seat.domain.ScheduleSeatStatus
import com.github.copyinaction.seat.domain.SeatPosition
import com.github.copyinaction.seat.repository.ScheduleSeatStatusRepository
import com.github.copyinaction.seat.service.SseService
import com.github.copyinaction.venue.util.SeatingChartParser
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

/**
 * 공용 체험 계정(`demo-user-01~03@smarter.dev`) 예매 내역 관리
 *
 * 외부 방문자가 로그인해서 `/mypage/bookings` 를 체험할 수 있도록
 * 결제 완료/취소 샘플 예매를 만들어 두고, 방문자가 쌓은 데이터는 주기적으로 초기화한다.
 */
@Service
@ConditionalOnDemoSeed
class DemoUserBookingSeedService(
    private val userSeedProvider: DemoUserSeedProvider,
    private val bookingRepository: BookingRepository,
    private val paymentRepository: PaymentRepository,
    private val couponUsageRepository: CouponUsageRepository,
    private val scheduleSeatStatusRepository: ScheduleSeatStatusRepository,
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val bookingFactory: DemoBookingFactory,
    private val seatingChartParser: SeatingChartParser,
    private val sseService: SseService,
    private val passwordEncoder: PasswordEncoder
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        /** 공용 계정 1개당 보관 가능한 예매 상한 (초과분은 오래된 것부터 삭제) */
        const val MAX_BOOKINGS_PER_USER = 15

        private val SAMPLE_PAYMENT_METHODS = listOf(
            PaymentMethod.CREDIT_CARD,
            PaymentMethod.KAKAO_PAY,
            PaymentMethod.VIRTUAL_ACCOUNT
        )
    }

    /**
     * 공용 체험 계정에 예매 내역이 하나라도 있는지 여부 (기동 시 최초 시딩 판단용)
     */
    @Transactional(readOnly = true)
    fun hasAnyBooking(): Boolean {
        val userIds = userSeedProvider.findDemoUsers().map { it.id }
        if (userIds.isEmpty()) return false
        return userIds.any { bookingRepository.countBySiteUser_Id(it) > 0 }
    }

    /**
     * 공용 체험 계정 3개의 예매/결제 내역을 FK 안전 순서로 모두 삭제하고 샘플을 다시 시딩한다.
     *
     * @return 새로 생성된 샘플 예매 수
     */
    @Transactional
    fun resetAndReseed(): Int {
        val users = userSeedProvider.findDemoUsers()
        if (users.isEmpty()) {
            log.warn("[DemoSeed] 공용 체험 계정이 없어 초기화를 건너뜁니다.")
            return 0
        }
        val userIds = users.map { it.id }

        // 방문자가 바꿨을 수 있는 비밀번호/이름을 원복
        users.forEachIndexed { index, user ->
            user.changePassword(userSeedProvider.demoPassword(), passwordEncoder)
            user.updateProfile("체험유저${index + 1}")
        }

        val bookings = bookingRepository.findAllBySiteUser_IdIn(userIds)
        // 쿠폰 사용 내역은 payment_id 외에 user_id 기준으로도 모두 제거
        couponUsageRepository.deleteAllByUserIdIn(userIds)
        deleteBookingsFully(bookings)
        // 예매에 연결되지 않은 채 남은 점유(PENDING 등)까지 정리
        scheduleSeatStatusRepository.deleteAllByHeldByIn(userIds)

        val created = seedSampleBookings(users)
        log.info("[DemoSeed] 공용 체험 계정 초기화 완료: 삭제 {}건, 샘플 재시딩 {}건", bookings.size, created)
        return created
    }

    /**
     * 공용 체험 계정당 예매가 [MAX_BOOKINGS_PER_USER]건을 초과하면 오래된 건부터 삭제한다.
     *
     * @return 삭제된 예매 수
     */
    @Transactional
    fun enforceBookingCap(): Int {
        var deleted = 0
        for (user in userSeedProvider.findDemoUsers()) {
            val ordered = bookingRepository.findAllBySiteUser_IdOrderByCreatedAtAsc(user.id)
            val overflow = ordered.size - MAX_BOOKINGS_PER_USER
            if (overflow <= 0) continue

            val targets = ordered.take(overflow)
            deleteBookingsFully(targets)
            deleted += targets.size
            log.info("[DemoSeed] 공용 계정 {} 예매 상한 초과: 오래된 {}건 삭제", user.email, targets.size)
        }
        return deleted
    }

    /**
     * 예매와 연결된 데이터를 FK 안전 순서로 삭제한다.
     *
     * 순서: 쿠폰 사용 내역 -> 결제(결제 항목/할인 cascade) -> 좌석 상태 -> 예매 좌석/예매
     */
    private fun deleteBookingsFully(bookings: List<Booking>) {
        if (bookings.isEmpty()) return

        // 1~3. 결제 관련 (coupon_usages -> payment_items/payment_discounts -> payments)
        val payments = paymentRepository.findAllByBookingIdIn(bookings.mapNotNull { it.id })
        if (payments.isNotEmpty()) {
            couponUsageRepository.deleteAllByPaymentIdIn(payments.map { it.id })
            paymentRepository.deleteAll(payments)
            paymentRepository.flush()
        }

        // 4. 해당 예매가 점유한 좌석 상태 삭제 + 열려 있는 SSE 클라이언트에 해제 알림
        val releasedBySchedule = mutableMapOf<Long, MutableList<SeatPosition>>()
        for (booking in bookings) {
            if (booking.bookingStatus != BookingStatus.CONFIRMED && booking.bookingStatus != BookingStatus.PENDING) continue
            val scheduleId = booking.schedule.id
            for (seat in booking.bookingSeats) {
                val status = scheduleSeatStatusRepository.findByScheduleIdAndRowNumAndColNum(scheduleId, seat.row, seat.col)
                if (status != null && status.heldBy == booking.siteUser.id) {
                    scheduleSeatStatusRepository.delete(status)
                    releasedBySchedule.getOrPut(scheduleId) { mutableListOf() }.add(SeatPosition(seat.row, seat.col))
                }
            }
        }
        scheduleSeatStatusRepository.flush()

        // 5. booking_seat -> booking (cascade + orphanRemoval)
        bookingRepository.deleteAll(bookings)
        bookingRepository.flush()

        releasedBySchedule.forEach { (scheduleId, seats) -> sseService.sendReleased(scheduleId, seats) }
    }

    /**
     * 공용 계정마다 미래 회차 중 랜덤 3~4건을 골라 샘플 예매를 만든다.
     * - 결제 완료(CONFIRMED + Payment COMPLETED) 2~3건 (결제수단 다양화, 좌석 RESERVED 점유)
     * - 취소(CANCELLED + Payment CANCELLED) 1건 (좌석 점유 없음)
     */
    private fun seedSampleBookings(users: List<User>): Int {
        val now = LocalDateTime.now()
        val candidateIds = performanceScheduleRepository.findSeedableScheduleIds(now.plusDays(1), now.plusDays(28))
        if (candidateIds.isEmpty()) {
            log.warn("[DemoSeed] 샘플 예매를 만들 미래 회차가 없습니다. (회차 롤링 선행 필요)")
            return 0
        }

        val usedNumbers = bookingFactory.loadUsedBookingNumbers()
        var created = 0
        var methodCursor = 0

        for (user in users) {
            val confirmedCount = Random.nextInt(2, 4) // 2~3건
            val pickedIds = candidateIds.shuffled().take(confirmedCount + 1)

            pickedIds.forEachIndexed { index, scheduleId ->
                val schedule = performanceScheduleRepository.findById(scheduleId).orElse(null) ?: return@forEachIndexed
                val isCancelledSample = index == confirmedCount
                val method = SAMPLE_PAYMENT_METHODS[methodCursor++ % SAMPLE_PAYMENT_METHODS.size]
                try {
                    if (createSampleBooking(user, schedule, method, isCancelledSample, usedNumbers)) created++
                } catch (e: Exception) {
                    log.warn("[DemoSeed] 공용 계정 {} 샘플 예매 생성 실패 (scheduleId={}): {}", user.email, scheduleId, e.message)
                }
            }
        }
        return created
    }

    private fun createSampleBooking(
        user: User,
        schedule: PerformanceSchedule,
        method: PaymentMethod,
        cancelled: Boolean,
        usedNumbers: MutableSet<String>
    ): Boolean {
        val performance = schedule.performance
        val venue = performance.venue ?: return false
        val layout = seatingChartParser.parseLayout(venue.seatingChart) ?: return false
        val priceByGrade = schedule.ticketOptions.associate { it.seatGrade to it.price }
        val sellableLayout = layout.copy(rowGrades = layout.rowGrades.filterValues { it in priceByGrade })

        val occupied = scheduleSeatStatusRepository.findByScheduleId(schedule.id).map { Pair(it.rowNum, it.colNum) }
        val seats = DemoSeatAllocator(sellableLayout, occupied).allocateBestEffort(Random.nextInt(1, 3)) ?: return false

        val grade = seats.first().grade
        val booking = bookingFactory.create(user, schedule, usedNumbers)
        booking.addSeats(seats.map { Pair(it.row, it.col) }, grade, priceByGrade.getValue(grade))
        booking.confirm()
        if (cancelled) booking.cancel()
        bookingRepository.save(booking)

        // 결제 이력 (PaymentCompletedEvent 는 발행하지 않는다: 대시보드 통계에 반영하지 않기 위함)
        val payment = Payment.create(
            booking = booking,
            userId = user.id,
            paymentMethod = method,
            originalPrice = booking.totalPrice,
            bookingFee = performance.bookingFee ?: 0
        )
        booking.bookingSeats.forEach { seat ->
            payment.paymentItems.add(PaymentItem.from(payment, seat, performance, schedule))
        }
        payment.complete(pgProviderOf(method), "demo-${UUID.randomUUID()}")
        if (method == PaymentMethod.CREDIT_CARD) {
            payment.cardCompany = "신한카드"
            payment.cardNumberMasked = "1234-56**-****-7890"
            payment.installmentMonths = 0
        }
        if (cancelled) {
            payment.cancel("테스트 예매 취소")
        }
        paymentRepository.save(payment)

        if (!cancelled) {
            val statuses = seats.map { ScheduleSeatStatus.reserve(schedule, it.row, it.col, it.grade, user.id) }
            scheduleSeatStatusRepository.saveAll(statuses)
        }
        return true
    }

    private fun pgProviderOf(method: PaymentMethod): String = when (method) {
        PaymentMethod.KAKAO_PAY -> "KAKAO_PAY"
        else -> "TOSS_PAYMENTS"
    }
}
