package com.github.copyinaction.admin.domain

@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "admins")
class Admin(
    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    val id: Long = 0,

    @jakarta.persistence.Column(nullable = false, unique = true)
    var loginId: String,

    @jakarta.persistence.Column(nullable = false)
    var name: String,

    @jakarta.persistence.Column(nullable = false)
    var passwordHash: String,
) : com.github.copyinaction.domain.BaseEntity()