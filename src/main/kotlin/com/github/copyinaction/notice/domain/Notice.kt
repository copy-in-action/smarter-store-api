package com.github.copyinaction.notice.domain

import com.github.copyinaction.common.domain.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.Comment

@Entity
@Table(name = "notice")
@Comment("공지사항")
class Notice(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("공지사항 ID")
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Comment("카테고리")
    var category: NoticeCategory,

    @Column(columnDefinition = "TEXT", nullable = false)
    @Comment("내용")
    var content: String,

    @Column(nullable = false)
    @Comment("활성화 여부")
    var isActive: Boolean = true

) : BaseEntity() {

    companion object {
        fun create(
            category: NoticeCategory,
            content: String,
            isActive: Boolean = true
        ): Notice {
            return Notice(
                category = category,
                content = content,
                isActive = isActive
            )
        }
    }

    fun update(
        category: NoticeCategory,
        content: String,
        isActive: Boolean
    ) {
        this.category = category
        this.content = content
        this.isActive = isActive
    }
}
