package com.github.copyinaction.reservation.domain

import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.performance.domain.PerformanceSchedule
import com.github.copyinaction.performance.domain.TicketOption
import com.github.copyinaction.venue.domain.Seat
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "schedule_seat",
    uniqueConstraints = [UniqueConstraint(columnNames = ["schedule_id", "seat_id"])]
)
class ScheduleSeat(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    val schedule: PerformanceSchedule,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    val seat: Seat,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_option_id", nullable = false)
    val ticketOption: TicketOption,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: SeatStatus = SeatStatus.AVAILABLE,

    @Column(name = "held_until")
    var heldUntil: LocalDateTime? = null,

    @Column(name = "held_by_user_id")
    var heldByUserId: Long? = null,

    @Column(name = "held_by_session_id", length = 100)
    var heldBySessionId: String? = null,

    @Version
    val version: Long = 0

) : BaseEntity() {

    companion object {
        const val DEFAULT_HOLD_MINUTES = 10L
        const val MAX_SEATS_PER_USER = 4
    }

    fun hold(userId: Long?, sessionId: String?, holdMinutes: Long = DEFAULT_HOLD_MINUTES) {
        require(status == SeatStatus.AVAILABLE) { "이미 점유된 좌석입니다." }
        status = SeatStatus.HELD
        heldUntil = LocalDateTime.now().plusMinutes(holdMinutes)
        heldByUserId = userId
        heldBySessionId = sessionId
    }

    fun release() {
        status = SeatStatus.AVAILABLE
        heldUntil = null
        heldByUserId = null
        heldBySessionId = null
    }

    fun reserve() {
        require(status == SeatStatus.HELD || status == SeatStatus.AVAILABLE) {
            "예매할 수 없는 좌석 상태입니다."
        }
        status = SeatStatus.RESERVED
        heldUntil = null
        heldByUserId = null
        heldBySessionId = null
    }

    fun cancelReservation() {
        require(status == SeatStatus.RESERVED) { "예매된 좌석만 취소할 수 있습니다." }
        status = SeatStatus.AVAILABLE
    }

    fun isExpired(): Boolean =
        status == SeatStatus.HELD && heldUntil != null && LocalDateTime.now().isAfter(heldUntil)

    fun isHeldBy(userId: Long?, sessionId: String?): Boolean {
        if (status != SeatStatus.HELD) return false
        return when {
            userId != null && heldByUserId == userId -> true
            sessionId != null && heldBySessionId == sessionId -> true
            else -> false
        }
    }

    fun getDisplayName(): String = seat.getDisplayName()
}
