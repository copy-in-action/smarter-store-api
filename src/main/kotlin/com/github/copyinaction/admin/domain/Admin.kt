package com.github.copyinaction.admin.domain

import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import jakarta.persistence.*
import org.springframework.security.crypto.password.PasswordEncoder

@Entity
@Table(name = "admins")
class Admin(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    var loginId: String,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var passwordHash: String,
) : BaseEntity() {

    companion object {
        fun create(
            loginId: String,
            name: String,
            rawPassword: String,
            passwordEncoder: PasswordEncoder
        ): Admin {
            return Admin(
                loginId = loginId,
                name = name,
                passwordHash = passwordEncoder.encode(rawPassword)
            )
        }
    }

    fun validatePassword(rawPassword: String, passwordEncoder: PasswordEncoder) {
        if (!passwordEncoder.matches(rawPassword, this.passwordHash)) {
            throw CustomException(ErrorCode.ADMIN_LOGIN_FAILED)
        }
    }

    fun changePassword(rawPassword: String, passwordEncoder: PasswordEncoder) {
        this.passwordHash = passwordEncoder.encode(rawPassword)
    }

    fun updateProfile(name: String) {
        this.name = name
    }
}