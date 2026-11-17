package com.github.copyinaction.demo.service

import com.github.copyinaction.auth.domain.User
import com.github.copyinaction.booking.domain.Booking
import com.github.copyinaction.booking.repository.BookingRepository
import com.github.copyinaction.demo.config.ConditionalOnDemoSeed
import com.github.copyinaction.performance.domain.PerformanceSchedule
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 시딩용 Booking 생성 헬퍼
 *
 * `Booking.create()`는 예매번호를 `yyyyMMdd-난수9자리`로 만들기 때문에, 수만 건을 연속 시딩하면
 * (생일 역설로) unique 제약 충돌 가능성이 있다. 이미 사용된 번호 집합을 들고 있다가
 * 충돌하면 다시 생성해서 시딩 트랜잭션이 중간에 깨지지 않도록 한다.
 */
@Component
@ConditionalOnDemoSeed
class DemoBookingFactory(
    private val bookingRepository: BookingRepository
) {
    /**
     * 오늘 날짜 접두사를 가진 기존 예매번호를 로드한다. (시딩 1회 실행당 1번만 호출)
     */
    fun loadUsedBookingNumbers(): MutableSet<String> {
        val prefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-"
        return bookingRepository.findBookingNumbersByPrefix(prefix).toHashSet()
    }

    /**
     * 예매번호가 중복되지 않는 Booking을 생성한다.
     * 내부적으로 `Booking.create()`를 사용하므로 회차 예매 가능 기간 검증(`validateCanBook`)도 그대로 적용된다.
     */
    fun create(user: User, schedule: PerformanceSchedule, usedNumbers: MutableSet<String>): Booking {
        repeat(MAX_RETRY) {
            val booking = Booking.create(user, schedule)
            if (usedNumbers.add(booking.bookingNumber)) {
                return booking
            }
        }
        throw IllegalStateException("예매번호 중복으로 Booking 생성에 실패했습니다. (retry=$MAX_RETRY)")
    }

    companion object {
        private const val MAX_RETRY = 20
    }
}
