package com.github.copyinaction.venue.domain

import com.github.copyinaction.common.domain.BaseEntity
import jakarta.persistence.*
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

    var phoneNumber: String? = null,

    @OneToMany(mappedBy = "venue", cascade = [CascadeType.ALL], orphanRemoval = true)
    var seatCapacities: MutableList<VenueSeatCapacity> = mutableListOf()
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

    fun updateSeatingChart(seatingChart: String?, newCapacities: List<VenueSeatCapacity>?) {
        this.seatingChart = seatingChart
        
        newCapacities?.let { newItems ->
            // 1. 요청에 없는 등급은 삭제 (Orphan Removal)
            val newGrades = newItems.map { it.seatGrade }.toSet()
            this.seatCapacities.removeIf { it.seatGrade !in newGrades }

            // 2. 기존 등급은 업데이트, 없으면 추가
            newItems.forEach { newItem ->
                val existing = this.seatCapacities.find { it.seatGrade == newItem.seatGrade }
                if (existing != null) {
                    existing.capacity = newItem.capacity
                } else {
                    this.seatCapacities.add(newItem)
                }
            }
        }
    }
}