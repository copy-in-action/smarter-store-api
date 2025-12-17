package com.github.copyinaction.notice.service

import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.notice.domain.NoticeCategory
import com.github.copyinaction.notice.domain.TicketingNotice
import com.github.copyinaction.notice.dto.CreateTicketingNoticeRequest
import com.github.copyinaction.notice.dto.TicketingNoticeGroupResponse
import com.github.copyinaction.notice.dto.TicketingNoticeResponse
import com.github.copyinaction.notice.dto.UpdateTicketingNoticeRequest
import com.github.copyinaction.notice.repository.TicketingNoticeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TicketingNoticeService(
    private val ticketingNoticeRepository: TicketingNoticeRepository
) {

    fun getAllNotices(): List<TicketingNoticeResponse> {
        return ticketingNoticeRepository.findAllByOrderByDisplayOrderAsc()
            .map { TicketingNoticeResponse.from(it) }
    }

    fun getActiveNotices(): List<TicketingNoticeResponse> {
        return ticketingNoticeRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
            .map { TicketingNoticeResponse.from(it) }
    }

    fun getActiveNoticesByCategory(category: NoticeCategory): List<TicketingNoticeResponse> {
        return ticketingNoticeRepository.findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(category)
            .map { TicketingNoticeResponse.from(it) }
    }

    fun getActiveNoticesGroupedByCategory(): List<TicketingNoticeGroupResponse> {
        val notices = ticketingNoticeRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
        return notices
            .groupBy { it.category }
            .map { (category, categoryNotices) ->
                TicketingNoticeGroupResponse(
                    category = category,
                    categoryDescription = category.description,
                    notices = categoryNotices.map { TicketingNoticeResponse.from(it) }
                )
            }
            .sortedBy { it.category.ordinal }
    }

    fun getNoticeById(id: Long): TicketingNoticeResponse {
        val notice = findNoticeById(id)
        return TicketingNoticeResponse.from(notice)
    }

    @Transactional
    fun createNotice(request: CreateTicketingNoticeRequest): TicketingNoticeResponse {
        val notice = TicketingNotice.create(
            category = request.category,
            title = request.title,
            content = request.content,
            displayOrder = request.displayOrder,
            isActive = request.isActive
        )
        val savedNotice = ticketingNoticeRepository.save(notice)
        return TicketingNoticeResponse.from(savedNotice)
    }

    @Transactional
    fun updateNotice(id: Long, request: UpdateTicketingNoticeRequest): TicketingNoticeResponse {
        val notice = findNoticeById(id)
        notice.update(
            category = request.category,
            title = request.title,
            content = request.content,
            displayOrder = request.displayOrder,
            isActive = request.isActive
        )
        return TicketingNoticeResponse.from(notice)
    }

    @Transactional
    fun deleteNotice(id: Long) {
        val notice = findNoticeById(id)
        ticketingNoticeRepository.delete(notice)
    }

    private fun findNoticeById(id: Long): TicketingNotice {
        return ticketingNoticeRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.TICKETING_NOTICE_NOT_FOUND) }
    }
}
