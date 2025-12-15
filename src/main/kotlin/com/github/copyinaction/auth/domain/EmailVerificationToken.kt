package com.github.copyinaction.auth.domain

import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import jakarta.persistence.*
import java.time.LocalDateTime
import kotlin.random.Random

@Entity
@Table(name = "email_verification_tokens")
class EmailVerificationToken(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false, unique = true)
    val token: String, // This will now be the 6-digit OTP

    @Column(nullable = false)
    val expiryDate: LocalDateTime,

    @Column(nullable = false)
    var isConfirmed: Boolean = false // New field to track if OTP has been confirmed

) : BaseEntity() {

    companion object {
        private const val EXPIRY_MINUTES = 5L // OTP usually has a shorter expiry
        private const val OTP_LENGTH = 6
        private val CHARS = ('0'..'9') + ('A'..'Z') + ('a'..'z') // Alphanumeric characters for OTP

        fun create(email: String): EmailVerificationToken {
            return EmailVerificationToken(
                email = email,
                token = generateOtp(),
                expiryDate = LocalDateTime.now().plusMinutes(EXPIRY_MINUTES)
            )
        }

        private fun generateOtp(): String {
            return (1..OTP_LENGTH)
                .map { CHARS.random(Random) }
                .joinToString("")
        }
    }

    fun isExpired(): Boolean {
        return expiryDate.isBefore(LocalDateTime.now())
    }

    fun confirm() {
        if (isConfirmed) {
            throw CustomException(ErrorCode.INVALID_EMAIL_VERIFICATION_TOKEN) // Or a more specific error
        }
        this.isConfirmed = true
    }

    fun validate(requestEmail: String, requestOtp: String? = null) { // Added requestOtp for potential future validation
        if (isExpired()) {
            throw CustomException(ErrorCode.EXPIRED_EMAIL_VERIFICATION_TOKEN)
        }
        if (this.email != requestEmail) {
            throw CustomException(ErrorCode.INVALID_EMAIL_VERIFICATION_TOKEN)
        }
        if (requestOtp != null && this.token != requestOtp) {
            throw CustomException(ErrorCode.INVALID_EMAIL_VERIFICATION_TOKEN)
        }
        if (isConfirmed) { // Ensure it hasn't been confirmed yet
            throw CustomException(ErrorCode.EMAIL_ALREADY_VERIFIED) // This token has already been used for confirmation
        }
    }
}
