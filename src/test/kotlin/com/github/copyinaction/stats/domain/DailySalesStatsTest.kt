package com.github.copyinaction.stats.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DailySalesStatsTest {

    @Nested
    @DisplayName("incrementSales 메서드")
    inner class IncrementSales {

        @Test
        @DisplayName("매출 증가 시 총 매출, 거래 수, 티켓 수, 할인 금액이 누적된다")
        fun incrementSalesAccumulates() {
            val stats = createStats()

            stats.incrementSales(amount = 50000, ticketCount = 2, discountAmount = 5000)

            assertThat(stats.totalSales).isEqualTo(50000)
            assertThat(stats.totalTransactions).isEqualTo(1)
            assertThat(stats.totalTickets).isEqualTo(2)
            assertThat(stats.totalDiscounts).isEqualTo(5000)
            assertThat(stats.netSales).isEqualTo(50000)
        }

        @Test
        @DisplayName("여러 번 호출 시 값이 누적된다")
        fun multipleIncrements() {
            val stats = createStats()

            stats.incrementSales(amount = 50000, ticketCount = 2, discountAmount = 5000)
            stats.incrementSales(amount = 30000, ticketCount = 1, discountAmount = 3000)

            assertThat(stats.totalSales).isEqualTo(80000)
            assertThat(stats.totalTransactions).isEqualTo(2)
            assertThat(stats.totalTickets).isEqualTo(3)
            assertThat(stats.totalDiscounts).isEqualTo(8000)
        }

        @Test
        @DisplayName("평균 주문 금액과 평균 티켓 가격이 계산된다")
        fun calculatesAverages() {
            val stats = createStats()

            stats.incrementSales(amount = 60000, ticketCount = 3, discountAmount = 0)

            assertThat(stats.avgOrderValue).isEqualTo(60000)
            assertThat(stats.avgTicketPrice).isEqualTo(20000)
        }

        @Test
        @DisplayName("updatedAt이 갱신된다")
        fun updatesTimestamp() {
            val stats = createStats()
            val initialUpdatedAt = stats.updatedAt

            Thread.sleep(10)
            stats.incrementSales(amount = 50000, ticketCount = 2, discountAmount = 0)

            assertThat(stats.updatedAt).isAfter(initialUpdatedAt)
        }
    }

    @Nested
    @DisplayName("recordRefund 메서드")
    inner class RecordRefund {

        @Test
        @DisplayName("환불 금액이 누적되고 순매출이 차감된다")
        fun recordRefundDeductsNetSales() {
            val stats = createStats()
            stats.incrementSales(amount = 100000, ticketCount = 4, discountAmount = 0)

            stats.recordRefund(amount = 25000)

            assertThat(stats.totalRefunds).isEqualTo(25000)
            assertThat(stats.netSales).isEqualTo(75000)
        }

        @Test
        @DisplayName("여러 번 환불 시 누적된다")
        fun multipleRefunds() {
            val stats = createStats()
            stats.incrementSales(amount = 100000, ticketCount = 4, discountAmount = 0)

            stats.recordRefund(amount = 25000)
            stats.recordRefund(amount = 25000)

            assertThat(stats.totalRefunds).isEqualTo(50000)
            assertThat(stats.netSales).isEqualTo(50000)
        }
    }

    @Nested
    @DisplayName("recordCancel 메서드")
    inner class RecordCancel {

        @Test
        @DisplayName("취소 건수가 증가한다")
        fun incrementsCancelledTransactions() {
            val stats = createStats()

            stats.recordCancel()
            stats.recordCancel()

            assertThat(stats.cancelledTransactions).isEqualTo(2)
        }
    }

    private fun createStats(date: LocalDate = LocalDate.now()): DailySalesStats {
        return DailySalesStats(date = date)
    }
}
