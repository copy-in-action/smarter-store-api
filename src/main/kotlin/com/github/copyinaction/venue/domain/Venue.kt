package com.github.copyinaction.venue.domain

@jakarta.persistence.Entity
class Venue(
    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    val id: Long = 0,

    var name: String,

    var address: String?,

    var seatingChartUrl: String?
) : com.github.copyinaction.common.domain.BaseEntity() {
    fun update(name: String, address: String?, seatingChartUrl: String?) {
        this.name = name
        this.address = address
        this.seatingChartUrl = seatingChartUrl
    }
}