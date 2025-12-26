package com.github.copyinaction.notice.service

import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.notice.domain.NoticeCategory
import com.github.copyinaction.notice.domain.Notice
import com.github.copyinaction.notice.dto.CreateNoticeRequest
import com.github.copyinaction.notice.dto.NoticeGroupResponse
import com.github.copyinaction.notice.dto.NoticeResponse
import com.github.copyinaction.notice.dto.UpdateNoticeRequest
import com.github.copyinaction.notice.repository.NoticeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class NoticeService(
    private val noticeRepository: NoticeRepository
) {

    fun getAllNotices(): List<NoticeResponse> {
        return noticeRepository.findAllByOrderByDisplayOrderAsc()
            .map { NoticeResponse.from(it) }
    }

    fun getActiveNotices(): List<NoticeResponse> {
        return noticeRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
            .map { NoticeResponse.from(it) }
    }

    fun getActiveNoticesByCategory(category: NoticeCategory): List<NoticeResponse> {
        return noticeRepository.findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(category)
            .map { NoticeResponse.from(it) }
    }

    fun getActiveNoticesGroupedByCategory(): List<NoticeGroupResponse> {
        val notices = noticeRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
        return notices
            .groupBy { it.category }
            .map { (category, categoryNotices) ->
                NoticeGroupResponse(
                    category = category,
                    categoryDescription = category.description,
                    notices = categoryNotices.map { NoticeResponse.from(it) }
                )
            }
            .sortedBy { it.category.ordinal }
    }

    fun getNoticeById(id: Long): NoticeResponse {
        val notice = findNoticeById(id)
        return NoticeResponse.from(notice)
    }

    @Transactional
    fun createNotice(request: CreateNoticeRequest): NoticeResponse {
        val notice = Notice.create(
            category = request.category,
            title = request.title,
            content = request.content,
            displayOrder = request.displayOrder,
            isActive = request.isActive
        )
        val savedNotice = noticeRepository.save(notice)
        return NoticeResponse.from(savedNotice)
    }

    @Transactional
    fun updateNotice(id: Long, request: UpdateNoticeRequest): NoticeResponse {
        val notice = findNoticeById(id)
        notice.update(
            category = request.category,
            title = request.title,
            content = request.content,
            displayOrder = request.displayOrder,
            isActive = request.isActive
        )
        return NoticeResponse.from(notice)
    }

    @Transactional
    fun deleteNotice(id: Long) {
        val notice = findNoticeById(id)
        noticeRepository.delete(notice)
    }

    private fun findNoticeById(id: Long): Notice {
        return noticeRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.NOTICE_NOT_FOUND) }
    }
}
