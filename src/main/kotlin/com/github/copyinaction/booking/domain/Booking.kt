package com.github.copyinaction.booking.domain

import com.github.copyinaction.auth.domain.User
import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.performance.domain.PerformanceSchedule
import jakarta.persistence.*
import org.hibernate.annotations.Comment
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "booking")
@Comment("예매 정보")
class Booking(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Comment("예매 ID")
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @Comment("예매자 ID")
    val user: User,

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
    var status: BookingStatus = BookingStatus.PENDING
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
            schedule: PerformanceSchedule,
            bookingNumber: String
        ): Booking {
            return Booking(
                user = user,
                schedule = schedule,
                bookingNumber = bookingNumber,
                expiresAt = LocalDateTime.now().plusMinutes(BOOKING_VALIDITY_MINUTES)
            )
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
        this.status = BookingStatus.CONFIRMED
    }

    fun cancel() {
        if (this.status != BookingStatus.PENDING && this.status != BookingStatus.CONFIRMED) {
             throw CustomException(ErrorCode.BOOKING_INVALID_STATUS, "취소할 수 없는 예매 상태입니다.")
        }
        this.status = BookingStatus.CANCELLED
    }

    fun expire() {
        if (this.status != BookingStatus.PENDING) {
            return // 이미 처리된 경우 무시
        }
        this.status = BookingStatus.EXPIRED
    }

    fun getRemainingSeconds(): Long {
        if (isExpired()) return 0
        return Duration.between(LocalDateTime.now(), this.expiresAt).seconds
    }

    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(this.expiresAt)
    }

    fun validateBookingIsMutable() {
        if (this.status != BookingStatus.PENDING) {
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

