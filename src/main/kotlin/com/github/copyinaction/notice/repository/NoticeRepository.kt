package com.github.copyinaction.notice.repository

import com.github.copyinaction.notice.domain.NoticeCategory
import com.github.copyinaction.notice.domain.Notice
import org.springframework.data.jpa.repository.JpaRepository

interface NoticeRepository : JpaRepository<Notice, Long> {
    fun findByIsActiveTrueOrderByDisplayOrderAsc(): List<Notice>
    fun findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(category: NoticeCategory): List<Notice>
    fun findAllByOrderByDisplayOrderAsc(): List<Notice>
}
