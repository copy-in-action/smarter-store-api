package com.github.copyinaction.venue.domain

import com.github.copyinaction.common.domain.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "seat",
    uniqueConstraints = [UniqueConstraint(columnNames = ["venue_id", "seat_row", "seat_number"])]
)
class Seat(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    val venue: Venue,

    @Column(nullable = false, length = 50)
    val section: String,

    @Column(name = "seat_row", nullable = false, length = 10)
    val row: String,

    @Column(name = "seat_number", nullable = false)
    val number: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_grade", nullable = false, length = 20)
    val seatGrade: SeatGrade = SeatGrade.STANDARD,

    @Column(name = "position_x", nullable = false)
    val positionX: Int,

    @Column(name = "position_y", nullable = false)
    val positionY: Int

) : BaseEntity() {

    fun getDisplayName(): String = "$section $row-$number"

    fun update(
        section: String,
        row: String,
        number: Int,
        seatGrade: SeatGrade,
        positionX: Int,
        positionY: Int
    ): Seat {
        return Seat(
            id = this.id,
            venue = this.venue,
            section = section,
            row = row,
            number = number,
            seatGrade = seatGrade,
            positionX = positionX,
            positionY = positionY
        )
    }
}
