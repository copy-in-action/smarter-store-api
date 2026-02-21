package com.github.copyinaction.notice.service

import com.github.copyinaction.notice.domain.Notice
import com.github.copyinaction.notice.domain.NoticeCategory
import com.github.copyinaction.notice.dto.CreateNoticeRequest
import com.github.copyinaction.notice.dto.UpdateNoticeRequest
import com.github.copyinaction.notice.repository.NoticeRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class NoticeServiceTest {

    @MockK
    private lateinit var noticeRepository: NoticeRepository

    @InjectMockKs
    private lateinit var noticeService: NoticeService

    @Test
    @DisplayName("공지사항을 생성할 때는 다른 공지사항 상태에 영향을 주지 않으며 비활성 상태로 생성된다")
    fun createNoticeDoesNotAffectOthers() {
        // Given
        val category = NoticeCategory.BOOKING_NOTICE
        val request = CreateNoticeRequest(
            category = category,
            content = "New Content"
        )
        
        every { noticeRepository.save(any()) } returnsArgument 0

        // When
        val result = noticeService.createNotice(request)

        // Then
        assertThat(result.isActive).isFalse()
        verify { noticeRepository.save(any()) }
        verify(exactly = 0) { noticeRepository.findByCategoryAndIsActiveTrue(any()) }
    }

    @Test
    @DisplayName("상태 수정을 통해 활성화하면 같은 카테고리의 다른 공지사항은 비활성화된다")
    fun updateStatusToActiveDeactivatesOthers() {
        // Given
        val category = NoticeCategory.BOOKING_NOTICE
        val targetNotice = Notice(id = 1, category = category, content = "Target", isActive = false)
        val otherActiveNotice = Notice(id = 2, category = category, content = "Other", isActive = true)

        every { noticeRepository.findById(1L) } returns Optional.of(targetNotice)
        every { noticeRepository.findByCategoryAndIsActiveTrue(category) } returns listOf(otherActiveNotice)

        // When
        noticeService.updateActiveStatus(1L, true)

        // Then
        assertThat(targetNotice.isActive).isTrue()
        assertThat(otherActiveNotice.isActive).isFalse()
    }

    @Test
    @DisplayName("상태 수정을 통해 비활성화하면 다른 공지사항 상태에는 영향을 주지 않는다")
    fun updateStatusToInactiveDoesNotAffectOthers() {
        // Given
        val category = NoticeCategory.BOOKING_NOTICE
        val targetNotice = Notice(id = 1, category = category, content = "Target", isActive = true)

        every { noticeRepository.findById(1L) } returns Optional.of(targetNotice)

        // When
        noticeService.updateActiveStatus(1L, false)

        // Then
        assertThat(targetNotice.isActive).isFalse()
        verify(exactly = 0) { noticeRepository.findByCategoryAndIsActiveTrue(any()) }
    }

    @Test
    @DisplayName("공지사항 수정 시 내용만 변경되며 카테고리와 상태는 유지된다")
    fun updateNoticeChangesOnlyContent() {
        // Given
        val id = 1L
        val originalCategory = NoticeCategory.BOOKING_NOTICE
        val originalContent = "Original Content"
        val originalIsActive = true
        val targetNotice = Notice(id = id, category = originalCategory, content = originalContent, isActive = originalIsActive)
        
        val request = UpdateNoticeRequest(content = "Updated Content")

        every { noticeRepository.findById(id) } returns Optional.of(targetNotice)

        // When
        val result = noticeService.updateNotice(id, request)

        // Then
        assertThat(result.content).isEqualTo("Updated Content")
        assertThat(result.category).isEqualTo(originalCategory)
        assertThat(result.isActive).isEqualTo(originalIsActive)
        verify { noticeRepository.findById(id) }
    }
}
