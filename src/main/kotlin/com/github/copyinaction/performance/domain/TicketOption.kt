package com.github.copyinaction.performance.domain

import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.venue.domain.SeatGrade
import jakarta.persistence.*
import org.hibernate.annotations.Comment

@Entity
@Table(name = "ticket_option")
@Comment("회차별 티켓 가격 정보")
class TicketOption(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("티켓 옵션 ID")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performance_schedule_id")
    @Comment("공연 회차 ID")
    val performanceSchedule: PerformanceSchedule,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("좌석 등급")
    val seatGrade: SeatGrade,

    @Column(nullable = false)
    @Comment("가격")
    val price: Int

) : BaseEntity()
