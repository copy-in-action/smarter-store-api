package com.github.copyinaction.reservation.domain

import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.performance.domain.PerformanceSchedule
import com.github.copyinaction.performance.domain.TicketOption
import jakarta.persistence.*
import java.math.BigDecimal

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

    fun canReserve(quantity: Int): Boolean = remainingQuantity >= quantity

    fun validateAndDecreaseStock(quantity: Int) {
        if (!canReserve(quantity)) {
            throw CustomException(ErrorCode.NOT_ENOUGH_SEATS)
        }
        remainingQuantity -= quantity
    }

    fun decreaseStock(quantity: Int) {
        require(remainingQuantity >= quantity) { "잔여 좌석이 부족합니다." }
        remainingQuantity -= quantity
    }

    fun increaseStock(quantity: Int) {
        require(remainingQuantity + quantity <= totalQuantity) { "총 좌석 수를 초과할 수 없습니다." }
        remainingQuantity += quantity
    }

    fun calculateTotalPrice(quantity: Int): BigDecimal {
        return ticketOption.price.multiply(quantity.toBigDecimal())
    }

    fun isSoldOut(): Boolean = remainingQuantity <= 0
}
