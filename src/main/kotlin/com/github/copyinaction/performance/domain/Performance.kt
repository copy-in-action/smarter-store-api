package com.github.copyinaction.performance.domain

import jakarta.persistence.*
import java.time.LocalDate

@Entity
class Performance(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var title: String,

    var description: String?,

    var category: String,

    var runningTime: Int?,

    var ageRating: String?,

    var mainImageUrl: String?,

    var visible: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    var venue: com.github.copyinaction.venue.domain.Venue?,

    var startDate: LocalDate,

    var endDate: LocalDate

) : com.github.copyinaction.common.domain.BaseEntity() {
    fun update(
        title: String,
        description: String?,
        category: String,
        runningTime: Int?,
        ageRating: String?,
        mainImageUrl: String?,
        visible: Boolean,
        venue: com.github.copyinaction.venue.domain.Venue?,
        startDate: LocalDate,
        endDate: LocalDate
    ) {
        this.title = title
        this.description = description
        this.category = category
        this.runningTime = runningTime
        this.ageRating = ageRating
        this.mainImageUrl = mainImageUrl
        this.visible = visible
        this.venue = venue
        this.startDate = startDate
        this.endDate = endDate
    }
}
