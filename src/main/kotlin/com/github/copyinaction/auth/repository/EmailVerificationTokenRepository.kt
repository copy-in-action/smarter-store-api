package com.github.copyinaction.auth.repository

import com.github.copyinaction.auth.domain.EmailVerificationToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface EmailVerificationTokenRepository : JpaRepository<EmailVerificationToken, Long> {
    fun findByToken(token: String): Optional<EmailVerificationToken>
    fun findByEmail(email: String): Optional<EmailVerificationToken>
    fun findByEmailAndToken(email: String, token: String): Optional<EmailVerificationToken>
    fun findByEmailAndIsConfirmed(email: String, isConfirmed: Boolean): Optional<EmailVerificationToken>
    fun deleteByEmail(email: String)
}
