package com.github.copyinaction.booking.domain

import com.github.copyinaction.auth.domain.User
import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.performance.domain.PerformanceSchedule
import com.github.copyinaction.venue.domain.SeatGrade
import jakarta.persistence.*
import org.hibernate.annotations.Comment
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.random.Random

@Entity
@Table(name = "booking")
@Comment("예매 정보")
class Booking(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    @Comment("예매 ID")
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @Comment("예매자 ID")
    val siteUser: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id")
    @Comment("공연 회차 ID")
    val schedule: PerformanceSchedule,

    @Column(nullable = false, unique = true)
    @Comment("예매 번호")
    val bookingNumber: String,

    @Column(nullable = false)
    @Comment("만료 시각")
    val expiresAt: LocalDateTime,

    @OneToMany(mappedBy = "booking", cascade = [CascadeType.ALL], orphanRemoval = true)
    val bookingSeats: MutableList<BookingSeat> = mutableListOf(),

) : BaseEntity() {
    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false)
    @Comment("예매 상태")
    var bookingStatus: BookingStatus = BookingStatus.PENDING
        private set

    @Column(nullable = false)
    @Comment("총 결제 금액")
    var totalPrice: Int = 0
        private set

    companion object {
        const val BOOKING_VALIDITY_MINUTES = 5L
        const val MAX_SEAT_COUNT = 4

        fun create(
            user: User,
            schedule: PerformanceSchedule
        ): Booking {
            schedule.validateCanBook()
            
            return Booking(
                siteUser = user,
                schedule = schedule,
                bookingNumber = generateBookingNumber(),
                expiresAt = LocalDateTime.now().plusMinutes(BOOKING_VALIDITY_MINUTES)
            )
        }

        private fun generateBookingNumber(): String {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            val datePart = LocalDateTime.now().format(formatter)
            val randomPart = Random.nextInt(100_000_000, 999_999_999).toString()
            return "$datePart-$randomPart"
        }
    }

    /**
     * 여러 좌석을 한 번에 추가합니다.
     * @param seatDetails row, col 정보를 담은 Pair 리스트
     * @param grade 좌석 등급
     * @param price 좌석 가격
     */
    fun addSeats(
        seatDetails: List<Pair<Int, Int>>,
        grade: SeatGrade,
        price: Int
    ) {
        seatDetails.forEach { (row, col) ->
            val bookingSeat = BookingSeat(
                booking = this,
                section = BookingSeat.DEFAULT_SECTION,
                rowName = row.toString(),
                seatNumber = col,
                grade = grade,
                price = price
            )
            addSeat(bookingSeat)
        }
    }

    fun addSeat(seat: BookingSeat) {
        validateBookingIsMutable()
        if (bookingSeats.size >= MAX_SEAT_COUNT) {
            throw CustomException(ErrorCode.SEAT_LIMIT_EXCEEDED)
        }
        bookingSeats.add(seat)
        recalculateTotalPrice()
    }

    fun removeSeat(seat: BookingSeat) {
        validateBookingIsMutable()
        bookingSeats.remove(seat)
        recalculateTotalPrice()
    }

    fun confirm() {
        validateBookingIsMutable()
        this.bookingStatus = BookingStatus.CONFIRMED
    }

    fun cancel() {
        if (this.bookingStatus != BookingStatus.PENDING && this.bookingStatus != BookingStatus.CONFIRMED) {
             throw CustomException(ErrorCode.BOOKING_INVALID_STATUS, "취소할 수 없는 예매 상태입니다.")
        }
        this.bookingStatus = BookingStatus.CANCELLED
    }

    fun expire() {
        if (this.bookingStatus != BookingStatus.PENDING) {
            return // 이미 처리된 경우 무시
        }
        this.bookingStatus = BookingStatus.EXPIRED
    }

    fun getRemainingSeconds(): Long {
        if (isExpired()) return 0
        return Duration.between(LocalDateTime.now(), this.expiresAt).seconds
    }

    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(this.expiresAt)
    }

    fun validateBookingIsMutable() {
        if (this.bookingStatus != BookingStatus.PENDING) {
            throw CustomException(ErrorCode.BOOKING_INVALID_STATUS, "예매를 변경할 수 없는 상태입니다.")
        }
        if (isExpired()) {
            throw CustomException(ErrorCode.BOOKING_EXPIRED)
        }
    }
    
    private fun recalculateTotalPrice() {
        this.totalPrice = bookingSeats.sumOf { it.price }
    }
}

