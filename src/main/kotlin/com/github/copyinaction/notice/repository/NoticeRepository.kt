package com.github.copyinaction.notice.repository

import com.github.copyinaction.notice.domain.NoticeCategory
import com.github.copyinaction.notice.domain.Notice
import org.springframework.data.jpa.repository.JpaRepository

interface NoticeRepository : JpaRepository<Notice, Long> {
    fun findByIsActiveTrueOrderByIdDesc(): List<Notice>
    fun findByCategoryAndIsActiveTrue(category: NoticeCategory): List<Notice>
    fun findAllByOrderByIdDesc(): List<Notice>
}
