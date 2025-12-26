package com.github.copyinaction.admin.controller

import com.github.copyinaction.performance.dto.CreatePerformanceScheduleRequest
import com.github.copyinaction.performance.dto.PerformanceScheduleResponse
import com.github.copyinaction.performance.dto.UpdatePerformanceScheduleRequest
import com.github.copyinaction.performance.service.PerformanceScheduleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "admin-performance-schedule", description = "관리자용 공연 회차 CRUD API")
@RestController
@RequestMapping("/api/admin/performances")
@SecurityRequirement(name = "bearerAuth")
class AdminPerformanceScheduleController(
    private val performanceScheduleService: PerformanceScheduleService
) {
    @Operation(summary = "공연 회차 생성", description = "특정 공연에 대한 새로운 회차와 티켓 가격 옵션을 생성합니다.\n\n**권한: ADMIN**")
    @PostMapping("/{performanceId}/schedules")
    fun createSchedule(
        @Parameter(description = "공연 ID", required = true, example = "1") @PathVariable performanceId: Long,
        @Valid @RequestBody request: CreatePerformanceScheduleRequest
    ): ResponseEntity<PerformanceScheduleResponse> {
        val response = performanceScheduleService.createSchedule(performanceId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "단일 공연 회차 조회", description = "ID로 특정 공연 회차의 정보를 조회합니다.\n\n**권한: ADMIN**")
    @GetMapping("/schedules/{scheduleId}")
    fun getSchedule(
        @Parameter(description = "회차 ID", required = true, example = "1") @PathVariable scheduleId: Long
    ): ResponseEntity<PerformanceScheduleResponse> {
        val response = performanceScheduleService.getSchedule(scheduleId)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "특정 공연의 모든 회차 조회", description = "특정 공연에 대한 모든 회차 목록을 조회합니다.\n\n**권한: ADMIN**")
    @GetMapping("/{performanceId}/schedules")
    fun getAllSchedules(
        @Parameter(description = "공연 ID", required = true, example = "1") @PathVariable performanceId: Long
    ): ResponseEntity<List<PerformanceScheduleResponse>> {
        val responses = performanceScheduleService.getAllSchedules(performanceId)
        return ResponseEntity.ok(responses)
    }

    @Operation(summary = "공연 회차 수정", description = "특정 공연 회차의 정보를 수정합니다.\n\n**권한: ADMIN**")
    @PutMapping("/schedules/{scheduleId}")
    fun updateSchedule(
        @Parameter(description = "회차 ID", required = true, example = "1") @PathVariable scheduleId: Long,
        @Valid @RequestBody request: UpdatePerformanceScheduleRequest
    ): ResponseEntity<PerformanceScheduleResponse> {
        val response = performanceScheduleService.updateSchedule(scheduleId, request)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "공연 회차 삭제", description = "특정 공연 회차를 삭제합니다.\n\n**권한: ADMIN**")
    @DeleteMapping("/schedules/{scheduleId}")
    fun deleteSchedule(
        @Parameter(description = "회차 ID", required = true, example = "1") @PathVariable scheduleId: Long
    ): ResponseEntity<Unit> {
        performanceScheduleService.deleteSchedule(scheduleId)
        return ResponseEntity.noContent().build()
    }
}
