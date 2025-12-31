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
    var showDateTime: LocalDateTime,

    @Column(nullable = false)
    @Comment("티켓 판매 시작 일시")
    var saleStartDateTime: LocalDateTime,

    @OneToMany(mappedBy = "performanceSchedule", cascade = [CascadeType.ALL], orphanRemoval = true)
    val ticketOptions: MutableList<TicketOption> = mutableListOf()

) : BaseEntity() {

    companion object {
        fun create(
            performance: Performance,
            showDateTime: LocalDateTime,
            saleStartDateTime: LocalDateTime
        ): PerformanceSchedule {
            return PerformanceSchedule(
                performance = performance,
                showDateTime = truncateToMinute(showDateTime),
                saleStartDateTime = truncateToMinute(saleStartDateTime)
            )
        }

        /**
         * 초/나노초 절삭 - 중복 체크 정확도 및 데이터 일관성 확보
         */
        fun truncateToMinute(dateTime: LocalDateTime): LocalDateTime {
            return dateTime.withSecond(0).withNano(0)
        }
    }

    fun update(
        showDateTime: LocalDateTime,
        saleStartDateTime: LocalDateTime
    ) {
        this.showDateTime = truncateToMinute(showDateTime)
        this.saleStartDateTime = truncateToMinute(saleStartDateTime)
    }

    fun addTicketOption(ticketOption: TicketOption) {
        this.ticketOptions.add(ticketOption)
    }

    fun clearTicketOptions() {
        this.ticketOptions.clear()
    }
}
