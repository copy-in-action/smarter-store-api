package com.github.copyinaction.booking.domain

import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.performance.domain.PerformanceSchedule
import jakarta.persistence.*
import org.hibernate.annotations.Comment
import java.time.LocalDateTime

@Entity
@Table(
    name = "seat_lock",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_seat_lock_schedule_id_section_row_name_seat_number",
            columnNames = ["schedule_id", "section", "row_name", "seat_number"]
        )
    ],
    indexes = [
        Index(name = "idx_seat_lock_booking_id", columnList = "booking_id"),
        Index(name = "idx_seat_lock_expires_at", columnList = "expires_at")
    ]
)
@Comment("좌석 점유 잠금 정보")
class SeatLock(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id")
    @Comment("공연 회차 ID")
    val schedule: PerformanceSchedule,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id")
    @Comment("예매 ID")
    val booking: Booking,

    @Column(nullable = false, length = 10)
    @Comment("구역")
    val section: String,

    @Column(nullable = false, length = 10)
    @Comment("열")
    val rowName: String,

    @Column(nullable = false)
    @Comment("좌석 번호")
    val seatNumber: Int,

    @Column(nullable = false)
    @Comment("만료 시각")
    val expiresAt: LocalDateTime

) : BaseEntity() {

    companion object {
        fun create(
            schedule: PerformanceSchedule,
            booking: Booking,
            section: String,
            rowName: String,
            seatNumber: Int
        ): SeatLock {
            return SeatLock(
                schedule = schedule,
                booking = booking,
                section = section,
                rowName = rowName,
                seatNumber = seatNumber,
                expiresAt = LocalDateTime.now().plusMinutes(Booking.BOOKING_VALIDITY_MINUTES)
            )
        }
    }
}
