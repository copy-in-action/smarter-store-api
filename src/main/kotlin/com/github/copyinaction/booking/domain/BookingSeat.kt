package com.github.copyinaction.booking.domain

import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.venue.domain.SeatGrade
import jakarta.persistence.*
import org.hibernate.annotations.Comment

@Entity
@Table(
    name = "booking_seat",
    indexes = [Index(name = "idx_booking_seat_booking_id", columnList = "booking_id")]
)
@Comment("예매 좌석 정보")
class BookingSeat(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("좌석 등급")
    val grade: SeatGrade,

    @Column(nullable = false)
    @Comment("가격")
    val price: Int

) : BaseEntity() {

    companion object {
        const val DEFAULT_SECTION = "GENERAL"

        fun create(
            booking: Booking,
            section: String,
            rowName: String,
            seatNumber: Int,
            grade: SeatGrade,
            price: Int
        ): BookingSeat {
            val seat = BookingSeat(
                booking = booking,
                section = section,
                rowName = rowName,
                seatNumber = seatNumber,
                grade = grade,
                price = price
            )
            booking.addSeat(seat)
            return seat
        }
    }
}
