package com.github.copyinaction.reservation.dto

import com.github.copyinaction.reservation.domain.ScheduleSeat
import com.github.copyinaction.reservation.domain.SeatStatus
import com.github.copyinaction.venue.domain.SeatGrade
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(description = "회차별 좌석 상태 응답 DTO")
data class ScheduleSeatResponse(
    @Schema(description = "회차별 좌석 ID")
    val scheduleSeatId: Long,

    @Schema(description = "좌석 ID")
    val seatId: Long,

    @Schema(description = "좌석 열")
    val row: String,

    @Schema(description = "좌석 번호")
    val number: Int,

    @Schema(description = "구역")
    val section: String,

    @Schema(description = "표시 이름")
    val displayName: String,

    @Schema(description = "좌석배치도 X좌표")
    val positionX: Int,

    @Schema(description = "좌석배치도 Y좌표")
    val positionY: Int,

    @Schema(description = "좌석 등급")
    val seatGrade: SeatGrade,

    @Schema(description = "좌석 상태")
    val status: SeatStatus,

    @Schema(description = "좌석등급 ID")
    val ticketOptionId: Long,

    @Schema(description = "좌석등급명")
    val ticketOptionName: String,

    @Schema(description = "가격")
    val price: BigDecimal,

    @Schema(description = "내가 점유한 좌석인지 여부")
    val isMyHold: Boolean = false
) {
    companion object {
        fun from(scheduleSeat: ScheduleSeat, userId: Long?, sessionId: String?): ScheduleSeatResponse {
            return ScheduleSeatResponse(
                scheduleSeatId = scheduleSeat.id,
                seatId = scheduleSeat.seat.id,
                row = scheduleSeat.seat.row,
                number = scheduleSeat.seat.number,
                section = scheduleSeat.seat.section,
                displayName = scheduleSeat.getDisplayName(),
                positionX = scheduleSeat.seat.positionX,
                positionY = scheduleSeat.seat.positionY,
                seatGrade = scheduleSeat.seat.seatGrade,
                status = scheduleSeat.status,
                ticketOptionId = scheduleSeat.ticketOption.id,
                ticketOptionName = scheduleSeat.ticketOption.name,
                price = scheduleSeat.ticketOption.price,
                isMyHold = scheduleSeat.isHeldBy(userId, sessionId)
            )
        }
    }
}

@Schema(description = "회차별 좌석 목록 응답 DTO (좌석배치도용)")
data class ScheduleSeatMapResponse(
    @Schema(description = "회차 ID")
    val scheduleId: Long,

    @Schema(description = "공연 제목")
    val performanceTitle: String,

    @Schema(description = "공연 일시")
    val showDatetime: LocalDateTime,

    @Schema(description = "공연장 이름")
    val venueName: String,

    @Schema(description = "구역별 좌석 목록")
    val sections: List<ScheduleSectionSeatsResponse>,

    @Schema(description = "좌석 상태 요약")
    val summary: SeatStatusSummary
)

@Schema(description = "구역별 회차 좌석 목록")
data class ScheduleSectionSeatsResponse(
    @Schema(description = "구역명")
    val name: String,

    @Schema(description = "좌석 목록")
    val seats: List<ScheduleSeatResponse>
)

@Schema(description = "좌석 상태 요약")
data class SeatStatusSummary(
    @Schema(description = "총 좌석 수")
    val total: Int,

    @Schema(description = "예매 가능 좌석 수")
    val available: Int,

    @Schema(description = "점유 중인 좌석 수")
    val held: Int,

    @Schema(description = "예매 완료 좌석 수")
    val reserved: Int
)

@Schema(description = "회차별 좌석 초기화 요청 DTO")
data class InitializeScheduleSeatsRequest(
    @field:NotNull(message = "좌석등급 매핑은 필수입니다.")
    @Schema(description = "좌석등급별 티켓옵션 ID 매핑 (SeatGrade -> TicketOption ID)")
    val seatGradeMapping: Map<SeatGrade, Long>
)

@Schema(description = "좌석 점유 요청 DTO")
data class HoldSeatsRequest(
    @field:NotEmpty(message = "좌석 ID 목록은 비워둘 수 없습니다.")
    @Schema(description = "점유할 좌석 ID 목록")
    val scheduleSeatIds: List<Long>,

    @Schema(description = "세션 ID (비회원용)")
    val sessionId: String? = null
)

@Schema(description = "좌석 점유 응답 DTO")
data class HoldSeatsResponse(
    @Schema(description = "성공 여부")
    val success: Boolean,

    @Schema(description = "점유된 좌석 목록")
    val heldSeats: List<HeldSeatInfo>,

    @Schema(description = "총 가격")
    val totalPrice: BigDecimal,

    @Schema(description = "점유 만료 시간")
    val expiresAt: LocalDateTime
)

@Schema(description = "점유된 좌석 정보")
data class HeldSeatInfo(
    @Schema(description = "회차별 좌석 ID")
    val scheduleSeatId: Long,

    @Schema(description = "표시 이름")
    val displayName: String,

    @Schema(description = "좌석등급명")
    val ticketOptionName: String,

    @Schema(description = "가격")
    val price: BigDecimal,

    @Schema(description = "점유 만료 시간")
    val heldUntil: LocalDateTime
)

@Schema(description = "좌석 해제 요청 DTO")
data class ReleaseSeatsRequest(
    @field:NotEmpty(message = "좌석 ID 목록은 비워둘 수 없습니다.")
    @Schema(description = "해제할 좌석 ID 목록")
    val scheduleSeatIds: List<Long>,

    @Schema(description = "세션 ID (비회원용)")
    val sessionId: String? = null
)

@Schema(description = "좌석 해제 응답 DTO")
data class ReleaseSeatsResponse(
    @Schema(description = "성공 여부")
    val success: Boolean,

    @Schema(description = "해제된 좌석 수")
    val releasedCount: Int
)
