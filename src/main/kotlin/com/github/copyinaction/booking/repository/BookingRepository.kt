package com.github.copyinaction.booking.repository

import com.github.copyinaction.booking.domain.Booking
import com.github.copyinaction.booking.domain.BookingStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface BookingRepository : JpaRepository<Booking, UUID> {

    /**
     * 사용자와 공연 회차, 예매 상태로 예매 정보를 찾습니다.
     * @param userId 사용자 ID
     * @param scheduleId 공연 회차 ID
     * @param bookingStatus 예매 상태
     * @return Booking?
     */
    fun findBySiteUser_IdAndSchedule_IdAndBookingStatus(userId: Long, scheduleId: Long, bookingStatus: BookingStatus): Booking?

    /**
     * 특정 상태와 만료 시간을 기준으로 모든 예매 정보를 찾습니다. (스케줄러용)
     * @param bookingStatus 예매 상태
     * @param expiresAt 만료 시각
     * @return List<Booking>
     */
    fun findAllByBookingStatusAndExpiresAtBefore(bookingStatus: BookingStatus, expiresAt: LocalDateTime): List<Booking>

    /**
     * 특정 회차에 특정 상태 목록에 포함되는 예매가 존재하는지 확인합니다.
     */
    fun existsBySchedule_IdAndBookingStatusIn(scheduleId: Long, bookingStatuses: Collection<BookingStatus>): Boolean

    /**
     * 특정 유저들의 모든 예매 조회 (데모 공용 계정 초기화용)
     */
    fun findAllBySiteUser_IdIn(userIds: Collection<Long>): List<Booking>

    /**
     * 특정 유저의 예매 건수 (데모 공용 계정 상한 보호용)
     */
    fun countBySiteUser_Id(userId: Long): Long

    /**
     * 특정 유저의 예매를 오래된 순으로 조회 (데모 공용 계정 상한 보호용)
     */
    fun findAllBySiteUser_IdOrderByCreatedAtAsc(userId: Long): List<Booking>

    /**
     * 특정 회차에서 특정 유저들이 보유한 특정 상태의 예매 조회 (데모 점유율 관리용)
     */
    fun findAllBySchedule_IdAndSiteUser_IdInAndBookingStatus(
        scheduleId: Long,
        userIds: Collection<Long>,
        bookingStatus: BookingStatus
    ): List<Booking>

    /**
     * 오늘 날짜 접두사를 가진 예매 번호 조회 (시딩 시 예매번호 중복 회피용)
     */
    @org.springframework.data.jpa.repository.Query("SELECT b.bookingNumber FROM Booking b WHERE b.bookingNumber LIKE CONCAT(:prefix, '%')")
    fun findBookingNumbersByPrefix(@org.springframework.data.repository.query.Param("prefix") prefix: String): List<String>

    /**
     * 특정 유저들의 특정 상태 예매 좌석 일괄 삭제 (데모 봇 이력 정리용)
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("""
        DELETE FROM BookingSeat bs WHERE bs.booking.id IN (
            SELECT b.id FROM Booking b
            WHERE b.siteUser.id IN :userIds AND b.bookingStatus IN :statuses
        )
    """)
    fun deleteSeatsByUserIdsAndStatuses(
        @org.springframework.data.repository.query.Param("userIds") userIds: Collection<Long>,
        @org.springframework.data.repository.query.Param("statuses") statuses: Collection<BookingStatus>
    ): Int

    /**
     * 특정 유저들의 특정 상태 예매 일괄 삭제 (데모 봇 이력 정리용)
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("""
        DELETE FROM Booking b
        WHERE b.siteUser.id IN :userIds AND b.bookingStatus IN :statuses
    """)
    fun deleteByUserIdsAndStatuses(
        @org.springframework.data.repository.query.Param("userIds") userIds: Collection<Long>,
        @org.springframework.data.repository.query.Param("statuses") statuses: Collection<BookingStatus>
    ): Int

    /**
     * 기준 시각 이전 회차의 특정 유저 예매 좌석 일괄 삭제 (데모 봇 이력 정리용)
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("""
        DELETE FROM BookingSeat bs WHERE bs.booking.id IN (
            SELECT b.id FROM Booking b
            WHERE b.siteUser.id IN :userIds
            AND b.schedule.id IN (
                SELECT ps.id FROM PerformanceSchedule ps WHERE ps.showDateTime < :cutoff
            )
        )
    """)
    fun deleteSeatsByUserIdsAndShowDateTimeBefore(
        @org.springframework.data.repository.query.Param("userIds") userIds: Collection<Long>,
        @org.springframework.data.repository.query.Param("cutoff") cutoff: LocalDateTime
    ): Int

    /**
     * 기준 시각 이전 회차의 특정 유저 예매 일괄 삭제 (데모 봇 이력 정리용)
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("""
        DELETE FROM Booking b
        WHERE b.siteUser.id IN :userIds
        AND b.schedule.id IN (
            SELECT ps.id FROM PerformanceSchedule ps WHERE ps.showDateTime < :cutoff
        )
    """)
    fun deleteByUserIdsAndShowDateTimeBefore(
        @org.springframework.data.repository.query.Param("userIds") userIds: Collection<Long>,
        @org.springframework.data.repository.query.Param("cutoff") cutoff: LocalDateTime
    ): Int

    /**
     * 사용자의 예매 내역을 조회합니다. (공연 및 회차 정보 포함)
     */
    @org.springframework.data.jpa.repository.Query("""
        SELECT DISTINCT b FROM Booking b
        JOIN FETCH b.schedule s
        JOIN FETCH s.performance p
        LEFT JOIN FETCH b.bookingSeats
        WHERE b.siteUser.id = :userId
        AND b.bookingStatus IN (
            com.github.copyinaction.booking.domain.BookingStatus.CONFIRMED,
            com.github.copyinaction.booking.domain.BookingStatus.CANCELLED
        )
        ORDER BY b.createdAt DESC
    """)
    fun findAllMyBookings(userId: Long): List<Booking>
    /**
     * 특정 회차의 모든 예매 내역을 조회합니다. (관리자용)
     */
    @org.springframework.data.jpa.repository.Query("""
        SELECT DISTINCT b FROM Booking b
        JOIN FETCH b.siteUser u
        LEFT JOIN FETCH b.bookingSeats
        WHERE b.schedule.id = :scheduleId
        ORDER BY b.createdAt DESC
    """)
    fun findAllByScheduleIdForAdmin(scheduleId: Long): List<Booking>
    /**
     * 예매 상세 정보를 조회합니다. (공연, 회차, 사용자, 좌석 정보 포함)
     */
    @org.springframework.data.jpa.repository.Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.schedule s
        JOIN FETCH s.performance p
        JOIN FETCH b.siteUser u
        LEFT JOIN FETCH b.bookingSeats
        WHERE b.id = :bookingId
    """)
    fun findByIdWithDetails(bookingId: UUID): Booking?
}
