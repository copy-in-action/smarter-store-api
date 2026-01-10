package com.github.copyinaction.seat.domain

import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.common.policy.BookingPolicy
import com.github.copyinaction.performance.domain.PerformanceSchedule
import com.github.copyinaction.venue.domain.SeatGrade
import jakarta.persistence.*
import org.hibernate.annotations.Comment
import java.time.LocalDateTime

/**
 * 회차별 좌석 상태
 * 점유 중(PENDING) 또는 예약 완료(RESERVED) 상태인 좌석만 저장
 */
@Entity
@Table(
    name = "schedule_seat_status",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_schedule_seat",
            columnNames = ["schedule_id", "row_num", "col_num"]
        )
    ],
    indexes = [
        Index(name = "idx_schedule_seat_status_schedule", columnList = "schedule_id"),
        Index(name = "idx_schedule_seat_status_held_until", columnList = "held_until"),
        Index(name = "idx_schedule_seat_status_grade", columnList = "schedule_id, seat_grade")
    ]
)
@Comment("회차별 좌석 상태")
class ScheduleSeatStatus(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id")
    @Comment("공연 회차 ID")
    val schedule: PerformanceSchedule,

    @Column(name = "row_num", nullable = false)
    @Comment("좌석 행 번호 (1부터 시작)")
    val rowNum: Int,

    @Column(name = "col_num", nullable = false)
    @Comment("좌석 열 번호 (1부터 시작)")
    val colNum: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_grade", nullable = false, length = 20)
    @Comment("좌석 등급")
    val seatGrade: SeatGrade,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("좌석 상태 (PENDING: 점유 중, RESERVED: 예약 완료)")
    var seatStatus: SeatStatus,

    @Column(name = "held_by")
    @Comment("점유 유저 ID")
    var heldBy: Long? = null,

    @Column(name = "held_until")
    @Comment("점유 만료 시간")
    var heldUntil: LocalDateTime? = null

) : BaseEntity() {

    companion object {
        /**
         * 좌석 점유 생성
         */
        fun hold(
            schedule: PerformanceSchedule,
            rowNum: Int,
            colNum: Int,
            seatGrade: SeatGrade,
            userId: Long
        ): ScheduleSeatStatus {
            return ScheduleSeatStatus(
                schedule = schedule,
                rowNum = rowNum,
                colNum = colNum,
                seatGrade = seatGrade,
                seatStatus = SeatStatus.PENDING,
                heldBy = userId,
                heldUntil = LocalDateTime.now().plusMinutes(BookingPolicy.BOOKING_HOLD_MINUTES)
            )
        }
    }

    /**
     * 점유가 만료되었는지 확인
     */
    fun isExpired(): Boolean {
        return seatStatus == SeatStatus.PENDING &&
               heldUntil != null &&
               LocalDateTime.now().isAfter(heldUntil)
    }

    /**
     * 예약 확정 (결제 완료)
     */
    fun reserve() {
        this.seatStatus = SeatStatus.RESERVED
        this.heldUntil = null
    }

    /**
     * 점유 연장
     */
    fun extendHold() {
        if (seatStatus == SeatStatus.PENDING) {
            this.heldUntil = LocalDateTime.now().plusMinutes(BookingPolicy.BOOKING_HOLD_MINUTES)
        }
    }
}
