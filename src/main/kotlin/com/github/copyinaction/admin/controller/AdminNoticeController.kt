package com.github.copyinaction.admin.controller

import com.github.copyinaction.audit.annotation.Auditable
import com.github.copyinaction.audit.domain.AuditAction
import com.github.copyinaction.audit.domain.AuditTargetType
import com.github.copyinaction.notice.dto.CreateNoticeRequest
import com.github.copyinaction.notice.dto.NoticeResponse
import com.github.copyinaction.notice.dto.NoticeStatusRequest
import com.github.copyinaction.notice.dto.UpdateNoticeRequest
import com.github.copyinaction.notice.service.NoticeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/notices")
@Tag(name = "admin-notice", description = "관리자 공지사항 API")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
class AdminNoticeController(
    private val noticeService: NoticeService
) {

    @GetMapping
    @Operation(summary = "[관리자] 전체 공지사항 목록 조회", description = "비활성화된 항목을 포함한 전체 목록을 조회합니다.\n\n**권한: ADMIN**")
    fun getAllNotices(): ResponseEntity<List<NoticeResponse>> {
        return ResponseEntity.ok(noticeService.getAllNotices())
    }

    @GetMapping("/grouped")
    @Operation(summary = "[관리자] 카테고리별 공지사항 목록 조회", description = "비활성화된 항목을 포함하여 카테고리별로 그룹화된 목록을 조회합니다.\n\n**권한: ADMIN**")
    fun getAllNoticesGrouped(): ResponseEntity<List<com.github.copyinaction.notice.dto.NoticeGroupResponse>> {
        return ResponseEntity.ok(noticeService.getAllNoticesGroupedByCategory())
    }

    @GetMapping("/{id}")
    @Operation(summary = "[관리자] 공지사항 상세 조회", description = "특정 공지사항의 상세 정보를 조회합니다.\n\n**권한: ADMIN**")
    fun getNoticeById(@PathVariable id: Long): ResponseEntity<NoticeResponse> {
        return ResponseEntity.ok(noticeService.getNoticeById(id))
    }

    @PostMapping
    @Operation(summary = "[관리자] 공지사항 생성", description = "새로운 공지사항을 생성합니다.\n\n**권한: ADMIN**\n\n**[Audit Log]** 이 작업은 감사 로그에 기록됩니다.")
    @Auditable(
        action = AuditAction.NOTICE_CREATE,
        targetType = AuditTargetType.NOTICE,
        includeRequestBody = true
    )
    fun createNotice(
        @Valid @RequestBody request: CreateNoticeRequest
    ): ResponseEntity<NoticeResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(noticeService.createNotice(request))
    }

    @PutMapping("/{id}")
    @Operation(summary = "[관리자] 공지사항 수정", description = "기존 공지사항을 수정합니다.\n\n**권한: ADMIN**\n\n**[Audit Log]** 이 작업은 감사 로그에 기록됩니다.")
    @Auditable(
        action = AuditAction.NOTICE_UPDATE,
        targetType = AuditTargetType.NOTICE,
        targetIdParam = "id",
        includeRequestBody = true
    )
    fun updateNotice(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateNoticeRequest
    ): ResponseEntity<NoticeResponse> {
        return ResponseEntity.ok(noticeService.updateNotice(id, request))
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "[관리자] 공지사항 상태 수정", description = "공지사항의 활성화 상태를 수정합니다. `isActive=true`로 전환 시 동일 카테고리의 다른 공지사항은 자동으로 비활성화됩니다.\n\n**권한: ADMIN**\n\n**[Audit Log]** 이 작업은 감사 로그에 기록됩니다.")
    @Auditable(
        action = AuditAction.NOTICE_UPDATE,
        targetType = AuditTargetType.NOTICE,
        targetIdParam = "id",
        includeRequestBody = true
    )
    fun updateNoticeStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: NoticeStatusRequest
    ): ResponseEntity<NoticeResponse> {
        return ResponseEntity.ok(noticeService.updateActiveStatus(id, request.isActive))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "[관리자] 공지사항 삭제", description = "공지사항을 삭제합니다.\n\n**권한: ADMIN**\n\n**[Audit Log]** 이 작업은 감사 로그에 기록됩니다.")
    @Auditable(
        action = AuditAction.NOTICE_DELETE,
        targetType = AuditTargetType.NOTICE,
        targetIdParam = "id"
    )
    fun deleteNotice(@PathVariable id: Long): ResponseEntity<Void> {
        noticeService.deleteNotice(id)
        return ResponseEntity.noContent().build()
    }
}
