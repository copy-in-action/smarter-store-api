package com.github.copyinaction.performance.controller

import com.github.copyinaction.performance.dto.CreatePerformanceScheduleRequest
import com.github.copyinaction.performance.dto.PerformanceScheduleResponse
import com.github.copyinaction.performance.service.PerformanceScheduleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@Tag(name = "공연 회차 관리 (관리자)", description = "관리자용 공연 회차 CRUD API")
@RestController
@RequestMapping("/api/admin/performances/{performanceId}/schedules")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
class PerformanceScheduleController(
    private val performanceScheduleService: PerformanceScheduleService
) {
    @Operation(summary = "공연 회차 생성", description = "특정 공연에 대한 새로운 회차와 티켓 가격 옵션을 생성합니다.\n\n**권한: ADMIN**")
    @PostMapping
    fun createSchedule(
        @PathVariable performanceId: Long,
        @Valid @RequestBody request: CreatePerformanceScheduleRequest
    ): ResponseEntity<PerformanceScheduleResponse> {
        val response = performanceScheduleService.createSchedule(performanceId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
