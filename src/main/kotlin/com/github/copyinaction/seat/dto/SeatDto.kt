package com.github.copyinaction.seat.dto

import com.github.copyinaction.seat.domain.ScheduleSeatStatus
import com.github.copyinaction.seat.domain.SeatStatus
import com.github.copyinaction.seat.domain.SeatPosition
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 좌석 위치 요청
 */
@Schema(description = "좌석 위치")
data class SeatPositionRequest(
    @Schema(description = "행 번호 (1부터 시작)", example = "1")
    val row: Int,

    @Schema(description = "열 번호 (1부터 시작)", example = "1")
    val col: Int
)

/**
 * 좌석 점유 요청
 */
@Schema(description = "좌석 점유 요청 DTO")
data class SeatHoldRequest(
    @Schema(description = "점유할 좌석 목록 (최대 4석)")
    val seats: List<SeatPositionRequest>
)

/**
 * 좌석 상태 응답
 */
@Schema(description = "좌석 상태 응답 DTO")
data class SeatStatusResponse(
    @Schema(description = "행 번호", example = "3")
    val row: Int,

    @Schema(description = "열 번호", example = "5")
    val col: Int,

    @Schema(description = "좌석 상태", example = "PENDING")
    val status: SeatStatus
) {
    companion object {
        fun from(seatStatus: ScheduleSeatStatus): SeatStatusResponse {
            return SeatStatusResponse(
                row = seatStatus.rowNum,
                col = seatStatus.colNum,
                status = seatStatus.seatStatus
            )
        }
    }
}

/**
 * 좌석 위치 응답
 */
@Schema(description = "좌석 위치 응답 DTO")
data class SeatPositionResponse(
    @Schema(description = "행 번호", example = "1")
    val row: Int,

    @Schema(description = "열 번호", example = "5")
    val col: Int
)

/**
 * 회차별 좌석 상태 목록 응답
 */
@Schema(description = "회차별 좌석 상태 목록 응답 DTO")
data class ScheduleSeatStatusResponse(
    @Schema(description = "회차 ID", example = "1")
    val scheduleId: Long,

    @Schema(description = "점유 중인 좌석 목록")
    val pending: List<SeatPositionResponse>,

    @Schema(description = "예약 완료된 좌석 목록")
    val reserved: List<SeatPositionResponse>
)

/**
 * 좌석 점유 응답
 */
@Schema(description = "좌석 점유 응답 DTO")
data class SeatHoldResponse(
    @Schema(description = "회차 ID", example = "1")
    val scheduleId: Long,

    @Schema(description = "점유된 좌석 목록")
    val heldSeats: List<SeatStatusResponse>,

    @Schema(description = "점유 만료 시간", example = "2025-01-15T14:30:00")
    val expiresAt: String
)

/**
 * SSE 이벤트 액션 타입
 */
@Schema(
    description = "SSE 좌석 이벤트 액션 (OCCUPIED: 점유됨, RELEASED: 해제됨, CONFIRMED: 확정됨)",
    enumAsRef = true
)
enum class SeatEventAction(val description: String) {
    OCCUPIED("점유됨"),
    RELEASED("해제됨"),
    CONFIRMED("확정됨")
}

/**
 * SSE 이벤트 메시지
 * - 좌석 상태 변경 시 구독자에게 전송되는 메시지
 */
data class SeatEventMessage(
    val action: SeatEventAction,
    val seats: List<SeatPosition>
) {
    companion object {
        fun occupied(seats: List<SeatPosition>) = SeatEventMessage(SeatEventAction.OCCUPIED, seats)
        fun released(seats: List<SeatPosition>) = SeatEventMessage(SeatEventAction.RELEASED, seats)
        fun confirmed(seats: List<SeatPosition>) = SeatEventMessage(SeatEventAction.CONFIRMED, seats)
    }
}
