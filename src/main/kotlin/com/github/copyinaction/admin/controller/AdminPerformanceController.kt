package com.github.copyinaction.admin.controller

import com.github.copyinaction.audit.annotation.Auditable
import com.github.copyinaction.audit.domain.AuditAction
import com.github.copyinaction.audit.domain.AuditTargetType
import com.github.copyinaction.common.exception.ErrorResponse
import com.github.copyinaction.performance.dto.CreatePerformanceRequest
import com.github.copyinaction.performance.dto.PerformanceResponse
import com.github.copyinaction.performance.dto.UpdatePerformanceRequest
import com.github.copyinaction.performance.service.PerformanceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@Tag(name = "admin-performance", description = "관리자용 공연 API - 공연 생성/수정/삭제")
@RestController
@RequestMapping("/api/admin/performances")
@SecurityRequirement(name = "bearerAuth")
class AdminPerformanceController(
    private val performanceService: PerformanceService
) {
    @Operation(summary = "전체 공연 목록 조회", description = "모든 공연 목록(비공개 포함)을 조회합니다.\n\n**권한: ADMIN**")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "공연 목록 조회 성공")
    )
    @GetMapping
    fun getAllPerformances(): ResponseEntity<List<PerformanceResponse>> {
        val performances = performanceService.getAllPerformances()
        return ResponseEntity.ok(performances)
    }

    @Operation(summary = "공연 생성", description = "새로운 공연 정보를 생성합니다.\n\n**권한: ADMIN**\n\n**[Audit Log]** 이 작업은 감사 로그에 기록됩니다.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "공연 생성 성공"),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 입력 값",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "접근 권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping
    @Auditable(
        action = AuditAction.PERFORMANCE_CREATE,
        targetType = AuditTargetType.PERFORMANCE,
        includeRequestBody = true
    )
    fun createPerformance(@Valid @RequestBody request: CreatePerformanceRequest): ResponseEntity<PerformanceResponse> {
        val performance = performanceService.createPerformance(request)
        val location = URI.create("/api/admin/performances/${performance.id}")
        return ResponseEntity.created(location).body(performance)
    }

    @Operation(summary = "공연 정보 수정", description = "특정 공연의 정보를 수정합니다.\n\n**권한: ADMIN**\n\n**[Audit Log]** 이 작업은 감사 로그에 기록됩니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "공연 정보 수정 성공"),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 입력 값",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "공연을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "접근 권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PutMapping("/{id}")
    @Auditable(
        action = AuditAction.PERFORMANCE_UPDATE,
        targetType = AuditTargetType.PERFORMANCE,
        targetIdParam = "id",
        includeRequestBody = true
    )
    fun updatePerformance(
        @Parameter(description = "수정할 공연의 ID", required = true, example = "1") @PathVariable id: Long,
        @Valid @RequestBody request: UpdatePerformanceRequest
    ): ResponseEntity<PerformanceResponse> {
        val performance = performanceService.updatePerformance(id, request)
        return ResponseEntity.ok(performance)
    }

    @Operation(summary = "공연 삭제", description = "특정 공연을 삭제합니다.\n\n**권한: ADMIN**\n\n**[Audit Log]** 이 작업은 감사 로그에 기록됩니다.")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "공연 삭제 성공"),
        ApiResponse(
            responseCode = "404",
            description = "공연을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "접근 권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @DeleteMapping("/{id}")
    @Auditable(
        action = AuditAction.PERFORMANCE_DELETE,
        targetType = AuditTargetType.PERFORMANCE,
        targetIdParam = "id"
    )
    fun deletePerformance(@Parameter(description = "삭제할 공연의 ID", required = true, example = "1") @PathVariable id: Long): ResponseEntity<Unit> {
        performanceService.deletePerformance(id)
        return ResponseEntity.noContent().build()
    }
}
