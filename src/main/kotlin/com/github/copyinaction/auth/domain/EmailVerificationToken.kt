package com.github.copyinaction.auth.domain

import com.github.copyinaction.common.domain.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "email_verification_tokens")
class EmailVerificationToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    var token: String,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(nullable = false)
    var expiryDate: LocalDateTime

) : BaseEntity() {
    fun isExpired(): Boolean {
        return expiryDate.isBefore(LocalDateTime.now())
    }
}
