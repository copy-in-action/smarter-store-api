package com.github.copyinaction.repository

import com.github.copyinaction.auth.domain.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.*

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): Optional<RefreshToken>

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.user.id = :userId")
    fun deleteByUserId(userId: Long)
}
