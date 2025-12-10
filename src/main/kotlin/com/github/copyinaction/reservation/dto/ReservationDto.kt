package com.github.copyinaction.reservation.dto

import com.github.copyinaction.reservation.domain.Reservation
import com.github.copyinaction.reservation.domain.ReservationStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(description = "예매 응답 DTO")
data class ReservationResponse(
    @Schema(description = "예매 ID", example = "1")
    val id: Long,

    @Schema(description = "예매 번호", example = "R20251225140001")
    val reservationNumber: String,

    @Schema(description = "공연명", example = "2025 HYOLYN CONCERT")
    val performanceTitle: String,

    @Schema(description = "공연 일시", example = "2025-12-25T14:00:00")
    val showDatetime: LocalDateTime,

    @Schema(description = "좌석 등급", example = "VIP석")
    val ticketOptionName: String,

    @Schema(description = "예매 수량", example = "2")
    val quantity: Int,

    @Schema(description = "총 결제 금액", example = "300000")
    val totalPrice: BigDecimal,

    @Schema(description = "예매자 이름", example = "홍길동")
    val userName: String,

    @Schema(description = "예매자 연락처", example = "010-1234-5678")
    val userPhone: String,

    @Schema(description = "예매자 이메일", example = "hong@example.com")
    val userEmail: String?,

    @Schema(description = "예매 상태", example = "CONFIRMED")
    val status: ReservationStatus,

    @Schema(description = "예매 일시", example = "2025-12-20T10:30:00")
    val reservedAt: LocalDateTime,

    @Schema(description = "확정 일시", example = "2025-12-20T10:31:00")
    val confirmedAt: LocalDateTime?,

    @Schema(description = "취소 일시")
    val cancelledAt: LocalDateTime?
) {
    companion object {
        fun from(reservation: Reservation): ReservationResponse {
            val stock = reservation.scheduleTicketStock!!
            val schedule = stock.schedule
            val ticketOption = stock.ticketOption
            val performance = schedule.performance

            return ReservationResponse(
                id = reservation.id,
                reservationNumber = reservation.reservationNumber,
                performanceTitle = performance.title,
                showDatetime = schedule.showDatetime,
                ticketOptionName = ticketOption.name,
                quantity = reservation.quantity,
                totalPrice = reservation.totalPrice,
                userName = reservation.userName,
                userPhone = reservation.userPhone,
                userEmail = reservation.userEmail,
                status = reservation.status,
                reservedAt = reservation.reservedAt,
                confirmedAt = reservation.confirmedAt,
                cancelledAt = reservation.cancelledAt
            )
        }
    }
}

@Schema(description = "예매 생성 요청 DTO")
data class CreateReservationRequest(
    @field:NotNull
    @Schema(description = "회차별 좌석등급 재고 ID", example = "1", required = true)
    val scheduleTicketStockId: Long,

    @field:NotNull
    @field:Min(1)
    @field:Max(10)
    @Schema(description = "예매 수량 (최대 10매)", example = "2", required = true)
    val quantity: Int,

    @field:NotBlank
    @field:Size(max = 100)
    @Schema(description = "예매자 이름", example = "홍길동", required = true)
    val userName: String,

    @field:NotBlank
    @field:Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
    @Schema(description = "예매자 연락처", example = "010-1234-5678", required = true)
    val userPhone: String,

    @field:Email
    @Schema(description = "예매자 이메일", example = "hong@example.com")
    val userEmail: String?
)

@Schema(description = "예매 조회 요청 DTO (비회원)")
data class ReservationLookupRequest(
    @field:NotBlank
    @Schema(description = "예매 번호", example = "R20251225140001", required = true)
    val reservationNumber: String,

    @field:NotBlank
    @Schema(description = "예매자 연락처", example = "010-1234-5678", required = true)
    val userPhone: String
)
