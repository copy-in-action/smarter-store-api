package com.github.copyinaction.auth.domain

import com.github.copyinaction.domain.BaseEntity
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

    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(expiryDate)
    }
}
