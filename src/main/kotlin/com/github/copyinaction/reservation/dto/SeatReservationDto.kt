package com.github.copyinaction.reservation.dto

import com.github.copyinaction.reservation.domain.Reservation
import com.github.copyinaction.reservation.domain.ReservationSeat
import com.github.copyinaction.reservation.domain.ReservationStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(description = "좌석 선택 예매 요청 DTO")
data class CreateSeatReservationRequest(
    @field:NotEmpty(message = "좌석 ID 목록은 비워둘 수 없습니다.")
    @Schema(description = "예매할 좌석 ID 목록")
    val scheduleSeatIds: List<Long>,

    @field:NotBlank(message = "예매자 이름은 비워둘 수 없습니다.")
    @field:Size(max = 100, message = "이름은 100자를 초과할 수 없습니다.")
    @Schema(description = "예매자 이름", example = "홍길동")
    val userName: String,

    @field:NotBlank(message = "연락처는 비워둘 수 없습니다.")
    @field:Size(max = 20, message = "연락처는 20자를 초과할 수 없습니다.")
    @Schema(description = "예매자 연락처", example = "010-1234-5678")
    val userPhone: String,

    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    @field:Size(max = 255, message = "이메일은 255자를 초과할 수 없습니다.")
    @Schema(description = "예매자 이메일", example = "hong@example.com")
    val userEmail: String? = null,

    @Schema(description = "세션 ID (비회원용)")
    val sessionId: String? = null
)

@Schema(description = "좌석 예매 응답 DTO")
data class SeatReservationResponse(
    @Schema(description = "예매 ID")
    val id: Long,

    @Schema(description = "예매 번호")
    val reservationNumber: String,

    @Schema(description = "공연 제목")
    val performanceTitle: String,

    @Schema(description = "공연 일시")
    val showDatetime: LocalDateTime,

    @Schema(description = "공연장 이름")
    val venueName: String,

    @Schema(description = "예매된 좌석 목록")
    val seats: List<ReservedSeatInfo>,

    @Schema(description = "예매 수량")
    val quantity: Int,

    @Schema(description = "총 결제 금액")
    val totalPrice: BigDecimal,

    @Schema(description = "예매자 이름")
    val userName: String,

    @Schema(description = "예매자 연락처")
    val userPhone: String,

    @Schema(description = "예매자 이메일")
    val userEmail: String?,

    @Schema(description = "예매 상태")
    val status: ReservationStatus,

    @Schema(description = "예매 일시")
    val reservedAt: LocalDateTime,

    @Schema(description = "확정 일시")
    val confirmedAt: LocalDateTime?,

    @Schema(description = "취소 일시")
    val cancelledAt: LocalDateTime?
) {
    companion object {
        fun from(reservation: Reservation, reservationSeats: List<ReservationSeat>): SeatReservationResponse {
            val schedule = reservation.schedule!!
            val performance = schedule.performance
            val venue = performance.venue!!

            return SeatReservationResponse(
                id = reservation.id,
                reservationNumber = reservation.reservationNumber,
                performanceTitle = performance.title,
                showDatetime = schedule.showDatetime,
                venueName = venue.name,
                seats = reservationSeats.map { ReservedSeatInfo.from(it) },
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

@Schema(description = "예매된 좌석 정보")
data class ReservedSeatInfo(
    @Schema(description = "회차별 좌석 ID")
    val scheduleSeatId: Long,

    @Schema(description = "표시 이름")
    val displayName: String,

    @Schema(description = "구역")
    val section: String,

    @Schema(description = "열")
    val row: String,

    @Schema(description = "번호")
    val number: Int,

    @Schema(description = "좌석등급명")
    val ticketOptionName: String,

    @Schema(description = "가격")
    val price: BigDecimal
) {
    companion object {
        fun from(reservationSeat: ReservationSeat): ReservedSeatInfo {
            val scheduleSeat = reservationSeat.scheduleSeat
            val seat = scheduleSeat.seat
            val ticketOption = scheduleSeat.ticketOption

            return ReservedSeatInfo(
                scheduleSeatId = scheduleSeat.id,
                displayName = seat.getDisplayName(),
                section = seat.section,
                row = seat.row,
                number = seat.number,
                ticketOptionName = ticketOption.name,
                price = ticketOption.price
            )
        }
    }
}
