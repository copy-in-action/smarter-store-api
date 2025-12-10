package com.github.copyinaction.reservation.domain

import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.performance.domain.PerformanceSchedule
import com.github.copyinaction.performance.domain.TicketOption
import jakarta.persistence.*

@Entity
@Table(
    name = "schedule_ticket_stock",
    uniqueConstraints = [UniqueConstraint(columnNames = ["schedule_id", "ticket_option_id"])]
)
class ScheduleTicketStock(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    val schedule: PerformanceSchedule,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_option_id", nullable = false)
    val ticketOption: TicketOption,

    @Column(nullable = false)
    var totalQuantity: Int,

    @Column(nullable = false)
    var remainingQuantity: Int

) : BaseEntity() {

    fun decreaseStock(quantity: Int) {
        require(remainingQuantity >= quantity) { "잔여 좌석이 부족합니다." }
        remainingQuantity -= quantity
    }

    fun increaseStock(quantity: Int) {
        require(remainingQuantity + quantity <= totalQuantity) { "총 좌석 수를 초과할 수 없습니다." }
        remainingQuantity += quantity
    }

    fun isSoldOut(): Boolean = remainingQuantity <= 0
}
