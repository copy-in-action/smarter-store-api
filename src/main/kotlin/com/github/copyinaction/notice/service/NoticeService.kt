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
        return noticeRepository.findAllByOrderByIdDesc()
            .map { NoticeResponse.from(it) }
    }

    fun getAllNoticesGroupedByCategory(): List<NoticeGroupResponse> {
        val notices = noticeRepository.findAllByOrderByIdDesc()
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

    fun getActiveNoticesGroupedByCategory(): List<NoticeGroupResponse> {
        val notices = noticeRepository.findByIsActiveTrueOrderByIdDesc()
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
            content = request.content,
            isActive = false // 신규 생성 시 기본값 비활성
        )
        val savedNotice = noticeRepository.save(notice)
        return NoticeResponse.from(savedNotice)
    }

    @Transactional
    fun updateNotice(id: Long, request: UpdateNoticeRequest): NoticeResponse {
        val notice = findNoticeById(id)
        notice.update(
            content = request.content
        )
        return NoticeResponse.from(notice)
    }

    @Transactional
    fun updateActiveStatus(id: Long, isActive: Boolean): NoticeResponse {
        val notice = findNoticeById(id)
        
        if (isActive) {
            // true로 전환 시에만 시스템이 개입하여 동일 카테고리의 다른 항목을 비활성화
            deactivateOtherNotices(notice.category, id)
        }
        
        notice.isActive = isActive
        return NoticeResponse.from(notice)
    }

    @Transactional
    fun deleteNotice(id: Long) {
        val notice = findNoticeById(id)
        noticeRepository.delete(notice)
    }

    private fun deactivateOtherNotices(category: NoticeCategory, excludeId: Long? = null) {
        val activeNotices = noticeRepository.findByCategoryAndIsActiveTrue(category)
        activeNotices.forEach {
            if (excludeId == null || it.id != excludeId) {
                it.isActive = false
            }
        }
    }

    private fun findNoticeById(id: Long): Notice {
        return noticeRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.NOTICE_NOT_FOUND) }
    }
}
