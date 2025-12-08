package com.github.copyinaction.domain

import jakarta.persistence.*
import java.time.LocalDate

@Entity
class Performance(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var title: String,

    @Lob
    var description: String?,

    var category: String,

    var runningTime: Int?,

    var ageRating: String?,

    var mainImageUrl: String?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    var venue: Venue?,

    var startDate: LocalDate,

    var endDate: LocalDate,

    @OneToMany(mappedBy = "performance", cascade = [CascadeType.ALL], orphanRemoval = true)
    val schedules: MutableList<PerformanceSchedule> = mutableListOf(),

    @OneToMany(mappedBy = "performance", cascade = [CascadeType.ALL], orphanRemoval = true)
    val ticketOptions: MutableList<TicketOption> = mutableListOf()

) : BaseEntity() {
    fun update(
        title: String,
        description: String?,
        category: String,
        runningTime: Int?,
        ageRating: String?,
        mainImageUrl: String?,
        venue: Venue?,
        startDate: LocalDate,
        endDate: LocalDate
    ) {
        this.title = title
        this.description = description
        this.category = category
        this.runningTime = runningTime
        this.ageRating = ageRating
        this.mainImageUrl = mainImageUrl
        this.venue = venue
        this.startDate = startDate
        this.endDate = endDate
    }
}
