package com.github.copyinaction.notice.repository

import com.github.copyinaction.notice.domain.NoticeCategory
import com.github.copyinaction.notice.domain.TicketingNotice
import org.springframework.data.jpa.repository.JpaRepository

interface TicketingNoticeRepository : JpaRepository<TicketingNotice, Long> {
    fun findByIsActiveTrueOrderByDisplayOrderAsc(): List<TicketingNotice>
    fun findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(category: NoticeCategory): List<TicketingNotice>
    fun findAllByOrderByDisplayOrderAsc(): List<TicketingNotice>
}
