package com.github.copyinaction.reservation.domain

import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.performance.domain.PerformanceSchedule
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Entity
@Table(name = "reservation")
class Reservation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 20)
    val reservationNumber: String,

    // 기존 수량 기반 예매용 (하위 호환성)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_ticket_stock_id")
    val scheduleTicketStock: ScheduleTicketStock? = null,

    // 좌석 기반 예매용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    val schedule: PerformanceSchedule? = null,

    // 좌석 기반 예매 - 예매된 좌석 목록
    @OneToMany(mappedBy = "reservation", cascade = [CascadeType.ALL], orphanRemoval = true)
    val reservationSeats: MutableList<ReservationSeat> = mutableListOf(),

    @Column
    val userId: Long? = null,

    @Column(length = 100)
    var userName: String,

    @Column(length = 20)
    var userPhone: String,

    @Column(length = 255)
    var userEmail: String?,

    @Column(nullable = false)
    val quantity: Int,

    @Column(nullable = false, precision = 12, scale = 2)
    val totalPrice: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ReservationStatus = ReservationStatus.PENDING,

    @Column(nullable = false)
    val reservedAt: LocalDateTime = LocalDateTime.now(),

    @Column
    var confirmedAt: LocalDateTime? = null,

    @Column
    var cancelledAt: LocalDateTime? = null

) : BaseEntity() {

    companion object {
        private val DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        fun generateReservationNumber(): String {
            val now = LocalDateTime.now()
            val dateTime = now.format(DATE_TIME_FORMAT)
            val random = (1000..9999).random()
            return "R$dateTime$random"
        }

        fun createWithStock(
            stock: ScheduleTicketStock,
            userId: Long?,
            userName: String,
            userPhone: String,
            userEmail: String?,
            quantity: Int
        ): Reservation {
            stock.validateAndDecreaseStock(quantity)
            val totalPrice = stock.calculateTotalPrice(quantity)

            return Reservation(
                reservationNumber = generateReservationNumber(),
                scheduleTicketStock = stock,
                userId = userId,
                userName = userName,
                userPhone = userPhone,
                userEmail = userEmail,
                quantity = quantity,
                totalPrice = totalPrice,
                status = ReservationStatus.PENDING
            )
        }

        fun createWithSeats(
            schedule: PerformanceSchedule,
            seats: List<ScheduleSeat>,
            userId: Long?,
            userName: String,
            userPhone: String,
            userEmail: String?
        ): Reservation {
            val totalPrice = seats.fold(BigDecimal.ZERO) { acc, seat ->
                acc.add(seat.ticketOption.price)
            }

            return Reservation(
                reservationNumber = generateReservationNumber(),
                schedule = schedule,
                userId = userId,
                userName = userName,
                userPhone = userPhone,
                userEmail = userEmail,
                quantity = seats.size,
                totalPrice = totalPrice,
                status = ReservationStatus.PENDING
            )
        }

        fun normalizePhone(phone: String): String = phone.replace("-", "")
    }

    fun isSeatBasedReservation(): Boolean = schedule != null && reservationSeats.isNotEmpty()

    fun matchesPhone(inputPhone: String): Boolean {
        return normalizePhone(this.userPhone) == normalizePhone(inputPhone)
    }

    fun confirm() {
        require(status == ReservationStatus.PENDING) { "대기 상태의 예매만 확정할 수 있습니다." }
        status = ReservationStatus.CONFIRMED
        confirmedAt = LocalDateTime.now()
    }

    fun cancel() {
        require(status != ReservationStatus.CANCELLED) { "이미 취소된 예매입니다." }
        status = ReservationStatus.CANCELLED
        cancelledAt = LocalDateTime.now()
    }
}

enum class ReservationStatus {
    PENDING,    // 예매 대기 (결제 대기)
    CONFIRMED,  // 예매 확정 (결제 완료)
    CANCELLED   // 예매 취소
}
