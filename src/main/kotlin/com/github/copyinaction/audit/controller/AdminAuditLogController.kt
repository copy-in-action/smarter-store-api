package com.github.copyinaction.audit.controller

import com.github.copyinaction.audit.domain.AuditAction
import com.github.copyinaction.audit.domain.AuditCategory
import com.github.copyinaction.audit.domain.AuditTargetType
import com.github.copyinaction.audit.dto.AuditLogDetailResponse
import com.github.copyinaction.audit.dto.AuditLogResponse
import com.github.copyinaction.audit.dto.AuditLogStatsResponse
import com.github.copyinaction.audit.service.AuditLogService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@Tag(name = "admin-audit-log", description = "관리자 감사 로그 API")
@RestController
@RequestMapping("/api/admin/audit-logs")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
class AdminAuditLogController(
    private val auditLogService: AuditLogService
) {

    @Operation(
        summary = "감사 로그 목록 조회",
        description = "다양한 필터를 사용하여 감사 로그 목록을 조회합니다.\n\n**권한: ADMIN**"
    )
    @GetMapping
    fun getAuditLogs(
        @Parameter(description = "사용자 ID") @RequestParam(required = false) userId: Long?,
        @Parameter(description = "액션 타입") @RequestParam(required = false) action: AuditAction?,
        @Parameter(description = "카테고리") @RequestParam(required = false) category: AuditCategory?,
        @Parameter(description = "대상 타입") @RequestParam(required = false) targetType: AuditTargetType?,
        @Parameter(description = "대상 ID") @RequestParam(required = false) targetId: String?,
        @Parameter(description = "시작 일시 (ISO-8601)") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime?,
        @Parameter(description = "종료 일시 (ISO-8601)") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime?,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<Page<AuditLogResponse>> {
        val result = auditLogService.getAuditLogs(
            userId = userId,
            action = action,
            category = category,
            targetType = targetType,
            targetId = targetId,
            from = from,
            to = to,
            pageable = pageable
        )
        return ResponseEntity.ok(result)
    }

    @Operation(
        summary = "감사 로그 상세 조회",
        description = "특정 감사 로그의 상세 정보를 조회합니다. 요청 본문 등 추가 정보가 포함됩니다.\n\n**권한: ADMIN**"
    )
    @GetMapping("/{id}")
    fun getAuditLogDetail(
        @Parameter(description = "감사 로그 ID") @PathVariable id: Long
    ): ResponseEntity<AuditLogDetailResponse> {
        return ResponseEntity.ok(auditLogService.getAuditLogDetail(id))
    }

    @Operation(
        summary = "특정 사용자의 감사 로그 조회",
        description = "특정 사용자의 모든 감사 로그를 조회합니다.\n\n**권한: ADMIN**"
    )
    @GetMapping("/users/{userId}")
    fun getAuditLogsByUser(
        @Parameter(description = "사용자 ID") @PathVariable userId: Long,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<Page<AuditLogResponse>> {
        return ResponseEntity.ok(auditLogService.getAuditLogsByUserId(userId, pageable))
    }

    @Operation(
        summary = "감사 로그 통계",
        description = "지정된 기간 동안의 감사 로그 통계를 조회합니다.\n\n**권한: ADMIN**"
    )
    @GetMapping("/stats")
    fun getAuditLogStats(
        @Parameter(description = "시작 일시 (ISO-8601)", required = true) @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime,
        @Parameter(description = "종료 일시 (ISO-8601)", required = true) @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime
    ): ResponseEntity<AuditLogStatsResponse> {
        return ResponseEntity.ok(auditLogService.getAuditLogStats(from, to))
    }
}
