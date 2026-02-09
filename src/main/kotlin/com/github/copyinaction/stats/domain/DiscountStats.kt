package com.github.copyinaction.stats.domain

import com.github.copyinaction.discount.domain.DiscountType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*
import org.hibernate.annotations.Comment
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "discount_stats")
@Comment("할인 수단별 통계")
@Schema(description = "할인 수단별 통계 응답")
class DiscountStats(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("통계 ID")
    @Schema(description = "통계 ID", example = "1")
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("할인 유형")
    @Schema(description = "할인 유형", example = "COUPON")
    val discountType: DiscountType,

    @Column(nullable = false)
    @Comment("날짜")
    @Schema(description = "날짜", example = "2026-01-05")
    val date: LocalDate,

    @Column(nullable = false)
    @Comment("사용 횟수")
    @Schema(description = "사용 횟수", example = "50")
    var usageCount: Int = 0,

    @Column(nullable = false)
    @Comment("총 할인 금액")
    @Schema(description = "총 할인 금액", example = "100000")
    var totalDiscountAmount: Long = 0,

    @Column(nullable = false)
    @Comment("평균 할인 금액")
    @Schema(description = "평균 할인 금액", example = "2000")
    var avgDiscountAmount: Int = 0,

    @Column(nullable = false)
    @Comment("수정일시")
    @Schema(description = "수정일시")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun increment(amount: Long) {
        this.usageCount++
        this.totalDiscountAmount += amount
        if (usageCount > 0) {
            this.avgDiscountAmount = (totalDiscountAmount / usageCount).toInt()
        }
        this.updatedAt = LocalDateTime.now()
    }
}
