package com.github.copyinaction.venue.domain

import com.github.copyinaction.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.hibernate.annotations.Comment

@Entity
class Venue(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var name: String,

    var address: String?,

    @Column(columnDefinition = "TEXT")
    @Comment("좌석 배치도 JSON")
    var seatingChart: String? = null,

    var phoneNumber: String? = null
) : BaseEntity() {

    companion object {
        fun create(
            name: String,
            address: String?,
            seatingChart: String? = null,
            phoneNumber: String? = null
        ): Venue {
            return Venue(
                name = name,
                address = address,
                seatingChart = seatingChart,
                phoneNumber = phoneNumber
            )
        }
    }

    fun update(name: String, address: String?, phoneNumber: String?) {
        this.name = name
        this.address = address
        this.phoneNumber = phoneNumber
    }

    fun updateSeatingChart(seatingChart: String?) {
        this.seatingChart = seatingChart
    }
}