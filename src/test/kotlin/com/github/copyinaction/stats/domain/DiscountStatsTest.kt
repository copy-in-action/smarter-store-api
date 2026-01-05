package com.github.copyinaction.stats.domain

import com.github.copyinaction.discount.domain.DiscountType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DiscountStatsTest {

    @Nested
    @DisplayName("increment 메서드")
    inner class Increment {

        @Test
        @DisplayName("사용 횟수와 총 할인 금액이 증가한다")
        fun incrementsCountAndAmount() {
            val stats = createStats()

            stats.increment(amount = 5000)

            assertThat(stats.usageCount).isEqualTo(1)
            assertThat(stats.totalDiscountAmount).isEqualTo(5000)
        }

        @Test
        @DisplayName("여러 번 호출 시 누적된다")
        fun multipleIncrements() {
            val stats = createStats()

            stats.increment(amount = 5000)
            stats.increment(amount = 3000)
            stats.increment(amount = 2000)

            assertThat(stats.usageCount).isEqualTo(3)
            assertThat(stats.totalDiscountAmount).isEqualTo(10000)
        }

        @Test
        @DisplayName("평균 할인 금액이 계산된다")
        fun calculatesAvgDiscountAmount() {
            val stats = createStats()

            stats.increment(amount = 6000)
            stats.increment(amount = 4000)

            assertThat(stats.avgDiscountAmount).isEqualTo(5000)
        }

        @Test
        @DisplayName("updatedAt이 갱신된다")
        fun updatesTimestamp() {
            val stats = createStats()
            val initialUpdatedAt = stats.updatedAt

            Thread.sleep(10)
            stats.increment(amount = 5000)

            assertThat(stats.updatedAt).isAfter(initialUpdatedAt)
        }
    }

    @Nested
    @DisplayName("할인 타입별 통계")
    inner class DiscountTypeStats {

        @Test
        @DisplayName("쿠폰 할인 통계를 기록할 수 있다")
        fun couponStats() {
            val stats = createStats(discountType = DiscountType.COUPON)

            stats.increment(amount = 10000)

            assertThat(stats.discountType).isEqualTo(DiscountType.COUPON)
            assertThat(stats.totalDiscountAmount).isEqualTo(10000)
        }

        @Test
        @DisplayName("프로모션 할인 통계를 기록할 수 있다")
        fun promotionStats() {
            val stats = createStats(discountType = DiscountType.PROMOTION)

            stats.increment(amount = 8000)

            assertThat(stats.discountType).isEqualTo(DiscountType.PROMOTION)
            assertThat(stats.totalDiscountAmount).isEqualTo(8000)
        }
    }

    private fun createStats(
        discountType: DiscountType = DiscountType.COUPON,
        date: LocalDate = LocalDate.now()
    ): DiscountStats {
        return DiscountStats(discountType = discountType, date = date)
    }
}
