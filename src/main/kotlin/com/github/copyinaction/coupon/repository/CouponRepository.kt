package com.github.copyinaction.coupon.repository

import com.github.copyinaction.coupon.domain.Coupon
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface CouponRepository : JpaRepository<Coupon, Long> {
    fun findByCode(code: String): Optional<Coupon>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id")
    fun findByIdWithLock(id: Long): Optional<Coupon>
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.code = :code")
    fun findByCodeWithLock(code: String): Optional<Coupon>
}
