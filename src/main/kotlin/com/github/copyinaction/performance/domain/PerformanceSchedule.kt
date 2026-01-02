package com.github.copyinaction.performance.domain

import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.venue.domain.SeatGrade
import jakarta.persistence.*
import org.hibernate.annotations.Comment
import java.time.LocalDateTime
import kotlin.math.max

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
            val schedule = PerformanceSchedule(
                performance = performance,
                showDateTime = truncateToMinute(showDateTime),
                saleStartDateTime = truncateToMinute(saleStartDateTime)
            )
            schedule.validate()
            return schedule
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
        validate()
    }

    /**
     * 티켓 옵션 일괄 추가
     */
    fun addTicketOptions(commands: List<TicketOptionCommand>) {
        commands.forEach { command ->
            val ticketOption = TicketOption(
                performanceSchedule = this,
                seatGrade = command.seatGrade,
                price = command.price,
                totalQuantity = command.totalQuantity
            )
            this.ticketOptions.add(ticketOption)
        }
    }

    private fun addTicketOption(ticketOption: TicketOption) {
        this.ticketOptions.add(ticketOption)
    }

    fun clearTicketOptions() {
        this.ticketOptions.clear()
    }

    /**
     * 등급별 잔여 좌석 계산
     * @param occupiedCounts 등급별 점유된 좌석 수 (Repository에서 조회하여 주입)
     */
    fun calculateRemainingSeats(occupiedCounts: Map<SeatGrade, Int>): List<TicketRemainingInfo> {
        return ticketOptions.map { option ->
            val occupied = occupiedCounts[option.seatGrade] ?: 0
            val remaining = max(0, option.totalQuantity - occupied)
            TicketRemainingInfo(
                seatGrade = option.seatGrade,
                price = option.price,
                remainingCount = remaining,
                totalCount = option.totalQuantity
            )
        }
    }

    private fun validate() {
        if (saleStartDateTime.isAfter(showDateTime)) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE, "판매 시작 일시는 공연 일시보다 이전이어야 합니다.")
        }
    }

    /**
     * 예매 가능 여부 검증
     */
    fun validateCanBook() {
        val now = LocalDateTime.now()
        if (now.isBefore(saleStartDateTime)) {
            throw CustomException(ErrorCode.INVALID_REQUEST, "아직 판매 시작 전인 회차입니다.")
        }
        if (now.isAfter(showDateTime)) {
            throw CustomException(ErrorCode.INVALID_REQUEST, "이미 공연이 시작되었거나 종료된 회차입니다.")
        }
    }
}

data class TicketOptionCommand(
    val seatGrade: SeatGrade,
    val price: Int,
    val totalQuantity: Int
)

data class TicketRemainingInfo(
    val seatGrade: SeatGrade,
    val price: Int,
    val remainingCount: Int,
    val totalCount: Int
)
