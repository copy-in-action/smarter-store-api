package com.github.copyinaction.stats.domain

import com.github.copyinaction.payment.domain.PaymentMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PaymentMethodStatsTest {

    @Nested
    @DisplayName("increment 메서드")
    inner class Increment {

        @Test
        @DisplayName("거래 건수와 총 금액이 증가한다")
        fun incrementsCountAndAmount() {
            val stats = createStats()

            stats.increment(amount = 50000)

            assertThat(stats.transactionCount).isEqualTo(1)
            assertThat(stats.totalAmount).isEqualTo(50000)
        }

        @Test
        @DisplayName("여러 번 호출 시 누적된다")
        fun multipleIncrements() {
            val stats = createStats()

            stats.increment(amount = 50000)
            stats.increment(amount = 30000)
            stats.increment(amount = 20000)

            assertThat(stats.transactionCount).isEqualTo(3)
            assertThat(stats.totalAmount).isEqualTo(100000)
        }

        @Test
        @DisplayName("평균 금액이 계산된다")
        fun calculatesAvgAmount() {
            val stats = createStats()

            stats.increment(amount = 60000)
            stats.increment(amount = 40000)

            assertThat(stats.avgAmount).isEqualTo(50000)
        }

        @Test
        @DisplayName("updatedAt이 갱신된다")
        fun updatesTimestamp() {
            val stats = createStats()
            val initialUpdatedAt = stats.updatedAt

            Thread.sleep(10)
            stats.increment(amount = 50000)

            assertThat(stats.updatedAt).isAfter(initialUpdatedAt)
        }
    }

    private fun createStats(
        paymentMethod: PaymentMethod = PaymentMethod.CREDIT_CARD,
        date: LocalDate = LocalDate.now()
    ): PaymentMethodStats {
        return PaymentMethodStats(paymentMethod = paymentMethod, date = date)
    }
}
