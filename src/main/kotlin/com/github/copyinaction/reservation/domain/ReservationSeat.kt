package com.github.copyinaction.reservation.domain

import com.github.copyinaction.common.domain.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "reservation_seat",
    uniqueConstraints = [UniqueConstraint(columnNames = ["reservation_id", "schedule_seat_id"])]
)
class ReservationSeat(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    val reservation: Reservation,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_seat_id", nullable = false)
    val scheduleSeat: ScheduleSeat

) : BaseEntity()
