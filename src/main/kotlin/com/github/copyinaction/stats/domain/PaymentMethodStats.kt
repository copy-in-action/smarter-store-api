package com.github.copyinaction.stats.domain

import com.github.copyinaction.payment.domain.PaymentMethod
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*
import org.hibernate.annotations.Comment
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "payment_method_stats")
@Comment("결제 수단별 통계")
@Schema(description = "결제 수단별 통계 응답")
class PaymentMethodStats(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("통계 ID")
    @Schema(description = "통계 ID", example = "1")
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("결제 수단")
    @Schema(description = "결제 수단", example = "CARD")
    val paymentMethod: PaymentMethod,

    @Column(nullable = false)
    @Comment("날짜")
    @Schema(description = "날짜", example = "2026-01-05")
    val date: LocalDate,

    @Column(nullable = false)
    @Comment("결제 건수")
    @Schema(description = "결제 건수", example = "100")
    var transactionCount: Int = 0,

    @Column(nullable = false)
    @Comment("총 결제 금액")
    @Schema(description = "총 결제 금액", example = "1000000")
    var totalAmount: Long = 0,

    @Column(nullable = false)
    @Comment("평균 결제 금액")
    @Schema(description = "평균 결제 금액", example = "10000")
    var avgAmount: Int = 0,

    @Column(nullable = false)
    @Comment("수정일시")
    @Schema(description = "수정일시")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun increment(amount: Long) {
        this.transactionCount++
        this.totalAmount += amount
        if (transactionCount > 0) {
            this.avgAmount = (totalAmount / transactionCount).toInt()
        }
        this.updatedAt = LocalDateTime.now()
    }
}
