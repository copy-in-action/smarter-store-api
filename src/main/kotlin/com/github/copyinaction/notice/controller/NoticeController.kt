package com.github.copyinaction.notice.controller

import com.github.copyinaction.notice.domain.NoticeCategory
import com.github.copyinaction.notice.dto.NoticeGroupResponse
import com.github.copyinaction.notice.dto.NoticeResponse
import com.github.copyinaction.notice.service.NoticeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notices")
@Tag(name = "notice", description = "공지사항 API")
class NoticeController(
    private val noticeService: NoticeService
) {

    @GetMapping("/grouped")
    @Operation(summary = "카테고리별 그룹화된 공지사항 조회", description = "활성화된 공지사항을 카테고리별로 그룹화하여 조회합니다.\n\n**권한: 누구나**")
    fun getActiveNoticesGrouped(): ResponseEntity<List<NoticeGroupResponse>> {
        return ResponseEntity.ok(noticeService.getActiveNoticesGroupedByCategory())
    }
}
