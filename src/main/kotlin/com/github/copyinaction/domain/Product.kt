package com.github.copyinaction.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var name: String,

    var price: Double,
) : BaseEntity() {
    fun update(name: String, price: Double) {
        this.name = name
        this.price = price
    }
}
