package com.github.copyinaction.venue.domain

import com.github.copyinaction.common.domain.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Venue(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var name: String,

    var address: String?,

    var seatingChartUrl: String?
) : BaseEntity() {

    companion object {
        fun create(
            name: String,
            address: String?,
            seatingChartUrl: String?
        ): Venue {
            return Venue(
                name = name,
                address = address,
                seatingChartUrl = seatingChartUrl
            )
        }
    }

    fun update(name: String, address: String?, seatingChartUrl: String?) {
        this.name = name
        this.address = address
        this.seatingChartUrl = seatingChartUrl
    }
}