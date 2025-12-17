package com.github.copyinaction.venue.domain

import com.github.copyinaction.common.domain.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.Comment

/**
 * 공연장별 등급별 허용 좌석수
 * 공연장마다 고정된 좌석 용량을 관리합니다.
 */
@Entity
@Table(
    name = "venue_seat_capacity",
    uniqueConstraints = [UniqueConstraint(columnNames = ["venue_id", "seat_grade"])]
)
@Comment("공연장 등급별 좌석 용량")
class VenueSeatCapacity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("좌석 용량 ID")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    @Comment("공연장 ID")
    val venue: Venue,

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_grade", nullable = false, length = 20)
    @Comment("좌석 등급 (VIP, R, S, A, B)")
    val seatGrade: SeatGrade,

    @Column(nullable = false)
    @Comment("허용 좌석 수")
    var capacity: Int

) : BaseEntity() {

    fun updateCapacity(capacity: Int) {
        require(capacity >= 0) { "좌석 수는 0 이상이어야 합니다." }
        this.capacity = capacity
    }
}
