package com.github.copyinaction.payment.domain

import com.github.copyinaction.booking.domain.BookingSeat
import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.performance.domain.Performance
import com.github.copyinaction.performance.domain.PerformanceSchedule
import com.github.copyinaction.venue.domain.SeatGrade
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "payment_items")
class PaymentItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    val payment: Payment,

    @Column(nullable = false)
    val performanceId: Long,

    @Column(nullable = false, length = 200)
    val performanceTitle: String,

    @Column(nullable = false)
    val scheduleId: Long,

    @Column(nullable = false)
    val showDateTime: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val seatGrade: SeatGrade,

    @Column(nullable = false, length = 50)
    val section: String,

    @Column(nullable = false)
    val rowNum: Int,

    @Column(nullable = false)
    val colNum: Int,

    @Column(nullable = false, length = 100)
    val seatLabel: String,

    @Column(nullable = false)
    val unitPrice: Int,

    @Column(nullable = false)
    var discountAmount: Int = 0,

    @Column(nullable = false)
    var finalPrice: Int

) : BaseEntity() {

    companion object {
        fun from(
            payment: Payment,
            bookingSeat: BookingSeat,
            performance: Performance,
            schedule: PerformanceSchedule
        ): PaymentItem {
            return PaymentItem(
                payment = payment,
                performanceId = performance.id!!,
                performanceTitle = performance.title,
                scheduleId = schedule.id!!,
                showDateTime = schedule.showDateTime,
                seatGrade = bookingSeat.grade,
                section = bookingSeat.section,
                rowNum = bookingSeat.row,
                colNum = bookingSeat.col,
                seatLabel = "${bookingSeat.section} ${bookingSeat.row}열 ${bookingSeat.col}번",
                unitPrice = bookingSeat.price,
                finalPrice = bookingSeat.price
            )
        }
    }

    fun applyDiscount(amount: Int) {
        this.discountAmount = amount
        this.finalPrice = unitPrice - amount
    }
}
