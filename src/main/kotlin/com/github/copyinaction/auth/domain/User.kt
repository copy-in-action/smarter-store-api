package com.github.copyinaction.auth.domain

import com.github.copyinaction.common.domain.BaseEntity
import jakarta.persistence.*

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

    @Column(nullable = false)
    var isEmailVerified: Boolean = false,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role,

) : BaseEntity()
