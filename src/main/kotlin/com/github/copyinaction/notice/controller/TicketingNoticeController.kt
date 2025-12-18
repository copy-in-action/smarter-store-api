package com.github.copyinaction.notice.controller

import com.github.copyinaction.notice.domain.NoticeCategory
import com.github.copyinaction.notice.dto.CreateTicketingNoticeRequest
import com.github.copyinaction.notice.dto.TicketingNoticeGroupResponse
import com.github.copyinaction.notice.dto.TicketingNoticeResponse
import com.github.copyinaction.notice.dto.UpdateTicketingNoticeRequest
import com.github.copyinaction.notice.service.TicketingNoticeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@Tag(name = "notice", description = "예매 안내사항 API")
class TicketingNoticeController(
    private val ticketingNoticeService: TicketingNoticeService
) {

    // ========== 사용자 API ==========

    @GetMapping("/ticketing-notices")
    @Operation(summary = "활성화된 예매 안내사항 목록 조회", description = "활성화된 모든 예매 안내사항을 정렬 순서대로 조회합니다.\n\n**권한: 누구나**")
    fun getActiveNotices(): ResponseEntity<List<TicketingNoticeResponse>> {
        return ResponseEntity.ok(ticketingNoticeService.getActiveNotices())
    }

    @GetMapping("/ticketing-notices/grouped")
    @Operation(summary = "카테고리별 그룹화된 예매 안내사항 조회", description = "활성화된 안내사항을 카테고리별로 그룹화하여 조회합니다.\n\n**권한: 누구나**")
    fun getActiveNoticesGrouped(): ResponseEntity<List<TicketingNoticeGroupResponse>> {
        return ResponseEntity.ok(ticketingNoticeService.getActiveNoticesGroupedByCategory())
    }

    @GetMapping("/ticketing-notices/category/{category}")
    @Operation(summary = "카테고리별 예매 안내사항 조회", description = "특정 카테고리의 활성화된 안내사항을 조회합니다.\n\n**권한: 누구나**")
    fun getActiveNoticesByCategory(
        @PathVariable category: NoticeCategory
    ): ResponseEntity<List<TicketingNoticeResponse>> {
        return ResponseEntity.ok(ticketingNoticeService.getActiveNoticesByCategory(category))
    }

    // ========== 관리자 API ==========

    @GetMapping("/admin/ticketing-notices")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[관리자] 전체 예매 안내사항 목록 조회", description = "비활성화된 항목을 포함한 전체 목록을 조회합니다.\n\n**권한: ADMIN**")
    fun getAllNotices(): ResponseEntity<List<TicketingNoticeResponse>> {
        return ResponseEntity.ok(ticketingNoticeService.getAllNotices())
    }

    @GetMapping("/admin/ticketing-notices/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[관리자] 예매 안내사항 상세 조회", description = "특정 안내사항의 상세 정보를 조회합니다.\n\n**권한: ADMIN**")
    fun getNoticeById(@PathVariable id: Long): ResponseEntity<TicketingNoticeResponse> {
        return ResponseEntity.ok(ticketingNoticeService.getNoticeById(id))
    }

    @PostMapping("/admin/ticketing-notices")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[관리자] 예매 안내사항 생성", description = "새로운 예매 안내사항을 생성합니다.\n\n**권한: ADMIN**")
    fun createNotice(
        @Valid @RequestBody request: CreateTicketingNoticeRequest
    ): ResponseEntity<TicketingNoticeResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ticketingNoticeService.createNotice(request))
    }

    @PutMapping("/admin/ticketing-notices/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[관리자] 예매 안내사항 수정", description = "기존 예매 안내사항을 수정합니다.\n\n**권한: ADMIN**")
    fun updateNotice(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateTicketingNoticeRequest
    ): ResponseEntity<TicketingNoticeResponse> {
        return ResponseEntity.ok(ticketingNoticeService.updateNotice(id, request))
    }

    @DeleteMapping("/admin/ticketing-notices/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[관리자] 예매 안내사항 삭제", description = "예매 안내사항을 삭제합니다.\n\n**권한: ADMIN**")
    fun deleteNotice(@PathVariable id: Long): ResponseEntity<Void> {
        ticketingNoticeService.deleteNotice(id)
        return ResponseEntity.noContent().build()
    }
}
