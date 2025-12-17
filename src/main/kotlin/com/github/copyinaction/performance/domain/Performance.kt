package com.github.copyinaction.performance.domain

import com.github.copyinaction.venue.domain.Venue
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
    var venue: Venue?,

    var startDate: LocalDate,

    var endDate: LocalDate

) : com.github.copyinaction.common.domain.BaseEntity() {

    companion object {
        fun create(
            title: String,
            description: String?,
            category: String,
            runningTime: Int?,
            ageRating: String?,
            mainImageUrl: String?,
            visible: Boolean = false,
            venue: Venue?,
            startDate: LocalDate,
            endDate: LocalDate
        ): Performance {
            return Performance(
                title = title,
                description = description,
                category = category,
                runningTime = runningTime,
                ageRating = ageRating,
                mainImageUrl = mainImageUrl,
                visible = visible,
                venue = venue,
                startDate = startDate,
                endDate = endDate
            )
        }
    }

    fun update(
        title: String,
        description: String?,
        category: String,
        runningTime: Int?,
        ageRating: String?,
        mainImageUrl: String?,
        visible: Boolean,
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
        this.visible = visible
        this.venue = venue
        this.startDate = startDate
        this.endDate = endDate
    }
}
