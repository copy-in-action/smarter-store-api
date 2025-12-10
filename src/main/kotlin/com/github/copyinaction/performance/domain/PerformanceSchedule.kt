package com.github.copyinaction.performance.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "performance_schedule")
class PerformanceSchedule(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id")
    var performance: Performance,

    var showDatetime: LocalDateTime,

    var saleStartDatetime: LocalDateTime?

) : com.github.copyinaction.common.domain.BaseEntity()
