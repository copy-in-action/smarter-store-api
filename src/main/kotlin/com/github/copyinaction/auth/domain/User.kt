package com.github.copyinaction.auth.domain

import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.common.encryption.EncryptedStringConverter
import com.github.copyinaction.config.jwt.JwtTokenProvider
import jakarta.persistence.*
import org.springframework.security.crypto.password.PasswordEncoder

@Entity
@Table(name = "users") // 'user' is a reserved keyword in some databases like PostgreSQL
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false)
    var username: String,

    @Column(nullable = false)
    var passwordHash: String,

    @Column(nullable = true)
    @Convert(converter = EncryptedStringConverter::class)
    var phoneNumber: String? = null,

    @Column(nullable = false)
    var isEmailVerified: Boolean = false,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role,

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val refreshTokens: MutableList<RefreshToken> = mutableListOf()

) : BaseEntity() {

    companion object {
        fun create(
            email: String,
            username: String,
            rawPassword: String,
            passwordEncoder: PasswordEncoder,
            phoneNumber: String?,
            isEmailVerified: Boolean = false,
            role: Role = Role.USER
        ): User {
            return User(
                email = email,
                username = username,
                passwordHash = passwordEncoder.encode(rawPassword),
                phoneNumber = phoneNumber,
                isEmailVerified = isEmailVerified,
                role = role
            )
        }
    }

    fun verifyEmail() {
        this.isEmailVerified = true
    }

    fun changePassword(rawPassword: String, passwordEncoder: PasswordEncoder) {
        this.passwordHash = passwordEncoder.encode(rawPassword)
    }

    fun updateProfile(username: String) {
        this.username = username
    }

    fun issueRefreshToken(jwtTokenProvider: JwtTokenProvider): RefreshToken {
        this.refreshTokens.clear()
        val refreshToken = RefreshToken.create(this, jwtTokenProvider)
        this.refreshTokens.add(refreshToken)
        return refreshToken
    }

    fun rotateRefreshToken(oldToken: RefreshToken, jwtTokenProvider: JwtTokenProvider): RefreshToken {
        oldToken.validateNotExpired()
        this.refreshTokens.remove(oldToken)
        val newToken = RefreshToken.create(this, jwtTokenProvider)
        this.refreshTokens.add(newToken)
        return newToken
    }
}
