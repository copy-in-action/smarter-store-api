package com.github.copyinaction.stats.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PerformanceSalesStatsTest {

    @Nested
    @DisplayName("update 메서드")
    inner class Update {

        @Test
        @DisplayName("매출, 티켓 수, 할인 금액이 누적된다")
        fun accumulatesValues() {
            val stats = createStats()

            stats.update(amount = 100000, ticketCount = 4, discountAmount = 10000, capacity = 100)

            assertThat(stats.totalRevenue).isEqualTo(100000)
            assertThat(stats.ticketsSold).isEqualTo(4)
            assertThat(stats.totalDiscountAmount).isEqualTo(10000)
            assertThat(stats.totalSeats).isEqualTo(100)
        }

        @Test
        @DisplayName("여러 번 호출 시 누적된다")
        fun multipleUpdates() {
            val stats = createStats()

            stats.update(amount = 50000, ticketCount = 2, discountAmount = 5000, capacity = 100)
            stats.update(amount = 75000, ticketCount = 3, discountAmount = 7500, capacity = 100)

            assertThat(stats.totalRevenue).isEqualTo(125000)
            assertThat(stats.ticketsSold).isEqualTo(5)
            assertThat(stats.totalDiscountAmount).isEqualTo(12500)
        }

        @Test
        @DisplayName("점유율이 올바르게 계산된다")
        fun calculatesOccupancyRate() {
            val stats = createStats()

            stats.update(amount = 50000, ticketCount = 25, discountAmount = 0, capacity = 100)

            assertThat(stats.occupancyRate).isEqualTo(25.0)
        }

        @Test
        @DisplayName("평균 티켓 가격이 계산된다")
        fun calculatesAvgTicketPrice() {
            val stats = createStats()

            stats.update(amount = 100000, ticketCount = 4, discountAmount = 0, capacity = 100)

            assertThat(stats.avgTicketPrice).isEqualTo(25000)
        }

        @Test
        @DisplayName("updatedAt이 갱신된다")
        fun updatesTimestamp() {
            val stats = createStats()
            val initialUpdatedAt = stats.updatedAt

            Thread.sleep(10)
            stats.update(amount = 50000, ticketCount = 2, discountAmount = 0, capacity = 100)

            assertThat(stats.updatedAt).isAfter(initialUpdatedAt)
        }
    }

    private fun createStats(
        performanceId: Long = 1L,
        date: LocalDate = LocalDate.now()
    ): PerformanceSalesStats {
        return PerformanceSalesStats(performanceId = performanceId, date = date)
    }
}
