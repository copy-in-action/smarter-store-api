package com.github.copyinaction.wishlist.domain

import com.github.copyinaction.auth.domain.User
import com.github.copyinaction.common.domain.BaseEntity
import com.github.copyinaction.performance.domain.Performance
import jakarta.persistence.*
import org.hibernate.annotations.Comment
import java.util.UUID

@Entity
@Table(
    name = "wishlist",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_wishlist_user_performance", columnNames = ["user_id", "performance_id"])
    ]
)
@Comment("찜 목록")
class Wishlist(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    @Comment("찜 ID")
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @Comment("사용자 ID")
    val siteUser: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performance_id")
    @Comment("공연 ID")
    val performance: Performance

) : BaseEntity() {
    companion object {
        fun create(user: User, performance: Performance): Wishlist {
            return Wishlist(
                siteUser = user,
                performance = performance
            )
        }
    }
}
