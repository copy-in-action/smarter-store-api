package com.github.copyinaction.performance.domain

import com.github.copyinaction.common.domain.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.Comment
import java.time.LocalDateTime

@Entity
@Table(name = "performance_schedule")
@Comment("공연 회차 정보")
class PerformanceSchedule(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("공연 회차 ID")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performance_id")
    @Comment("공연 ID")
    val performance: Performance,

    @Column(nullable = false)
    @Comment("공연 날짜 및 시간")
    val showDateTime: LocalDateTime,

    @Column(nullable = false)
    @Comment("티켓 판매 시작 일시")
    val saleStartDateTime: LocalDateTime

) : BaseEntity()
