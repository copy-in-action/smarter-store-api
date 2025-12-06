package com.github.copyinaction.domain

import jakarta.persistence.*

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
) : BaseEntity()
