package com.github.copyinaction.home.domain

import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.performance.domain.Performance
import jakarta.persistence.*
import org.hibernate.annotations.Comment

@Entity
@Table(
    name = "performance_home_tags",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_performance_home_tags_performance_tag",
            columnNames = ["performance_id", "tag"]
        )
    ],
    indexes = [
        Index(name = "idx_performance_home_tags_tag", columnList = "tag"),
        Index(name = "idx_performance_home_tags_tag_order", columnList = "tag, display_order")
    ]
)
@Comment("공연 홈 태그 매핑")
class PerformanceHomeTag private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    @Comment("공연 ID")
    val performance: Performance,

    @Enumerated(EnumType.STRING)
    @Column(name = "tag", nullable = false, length = 50)
    @Comment("홈 섹션 태그")
    val tag: HomeSectionTag,

    @Column(name = "display_order", nullable = false)
    @Comment("태그 내 노출 순서")
    var displayOrder: Int,

    @Column(name = "is_auto_tagged", nullable = false)
    @Comment("자동 태깅 여부 (지역 기반)")
    val isAutoTagged: Boolean = false
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    fun updateDisplayOrder(newOrder: Int) {
        this.displayOrder = newOrder
    }

    companion object {
        fun create(
            performance: Performance,
            tag: HomeSectionTag,
            displayOrder: Int,
            isAutoTagged: Boolean = false
        ): PerformanceHomeTag {
            return PerformanceHomeTag(
                performance = performance,
                tag = tag,
                displayOrder = displayOrder,
                isAutoTagged = isAutoTagged
            )
        }
    }
}
