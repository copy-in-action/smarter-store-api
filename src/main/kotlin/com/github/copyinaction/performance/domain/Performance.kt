package com.github.copyinaction.performance.domain

import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.venue.domain.Venue
import jakarta.persistence.*
import org.hibernate.annotations.Comment
import java.time.LocalDate

@Entity
class Performance(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var title: String,

    @Column(columnDefinition = "TEXT")
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

    var endDate: LocalDate,

    @Column(columnDefinition = "TEXT")
    @Comment("출연진")
    var actors: String? = null,

    @Column(length = 255)
    @Comment("기획사")
    var agency: String? = null,

    @Column(length = 255)
    @Comment("제작사")
    var producer: String? = null,

    @Column(length = 255)
    @Comment("주최")
    var host: String? = null,

    @Column(columnDefinition = "TEXT")
    @Comment("할인정보")
    var discountInfo: String? = null,

    @Column(columnDefinition = "TEXT")
    @Comment("이용안내")
    var usageGuide: String? = null,

    @Column(columnDefinition = "TEXT")
    @Comment("취소/환불규정")
    var refundPolicy: String? = null,

    @Column(length = 500)
    @Comment("상품상세 이미지 URL")
    var detailImageUrl: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @Comment("판매자/기획사 정보")
    var company: Company? = null,

    @Comment("예매 수수료")
    var bookingFee: Int? = null,

    @Column(columnDefinition = "TEXT")
    @Comment("배송 안내")
    var shippingGuide: String? = null

) : BaseEntity() {

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
            endDate: LocalDate,
            actors: String? = null,
            agency: String? = null,
            producer: String? = null,
            host: String? = null,
            discountInfo: String? = null,
            usageGuide: String? = null,
            refundPolicy: String? = null,
            detailImageUrl: String? = null,
            company: Company? = null,
            bookingFee: Int? = null,
            shippingGuide: String? = null
        ): Performance {
            val performance = Performance(
                title = title,
                description = description,
                category = category,
                runningTime = runningTime,
                ageRating = ageRating,
                mainImageUrl = mainImageUrl,
                visible = visible,
                venue = venue,
                startDate = startDate,
                endDate = endDate,
                actors = actors,
                agency = agency,
                producer = producer,
                host = host,
                discountInfo = discountInfo,
                usageGuide = usageGuide,
                refundPolicy = refundPolicy,
                detailImageUrl = detailImageUrl,
                company = company,
                bookingFee = bookingFee,
                shippingGuide = shippingGuide
            )
            performance.validate()
            return performance
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
        endDate: LocalDate,
        actors: String? = null,
        agency: String? = null,
        producer: String? = null,
        host: String? = null,
        discountInfo: String? = null,
        usageGuide: String? = null,
        refundPolicy: String? = null,
        detailImageUrl: String? = null,
        company: Company? = null,
        bookingFee: Int? = null,
        shippingGuide: String? = null
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
        this.actors = actors
        this.agency = agency
        this.producer = producer
        this.host = host
        this.discountInfo = discountInfo
        this.usageGuide = usageGuide
        this.refundPolicy = refundPolicy
        this.detailImageUrl = detailImageUrl
        this.company = company
        this.bookingFee = bookingFee
        this.shippingGuide = shippingGuide
        validate()
    }

    private fun validate() {
        if (endDate.isBefore(startDate)) {
            throw CustomException(ErrorCode.INVALID_INPUT_VALUE, "종료일은 시작일 이후여야 합니다.")
        }
        runningTime?.let {
            if (it <= 0) {
                throw CustomException(ErrorCode.INVALID_INPUT_VALUE, "러닝타임은 0보다 커야 합니다.")
            }
        }
        bookingFee?.let {
            if (it < 0) {
                throw CustomException(ErrorCode.INVALID_INPUT_VALUE, "예매 수수료는 0 이상이어야 합니다.")
            }
        }
    }
}