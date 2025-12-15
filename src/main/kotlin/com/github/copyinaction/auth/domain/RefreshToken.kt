package com.github.copyinaction.auth.domain

import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.config.jwt.JwtTokenProvider
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, unique = true)
    val token: String,

    @Column(nullable = false)
    val expiryDate: LocalDateTime,
) : BaseEntity() {

    companion object {
        fun create(user: User, jwtTokenProvider: JwtTokenProvider): RefreshToken {
            return RefreshToken(
                user = user,
                token = jwtTokenProvider.createRefreshToken(),
                expiryDate = jwtTokenProvider.getRefreshTokenExpiryDate()
            )
        }
    }

    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(expiryDate)
    }

    fun validateNotExpired() {
        if (isExpired()) {
            throw CustomException(ErrorCode.EXPIRED_REFRESH_TOKEN)
        }
    }
}
