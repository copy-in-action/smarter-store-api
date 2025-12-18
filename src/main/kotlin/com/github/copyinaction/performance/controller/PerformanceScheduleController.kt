package com.github.copyinaction.performance.controller

import com.github.copyinaction.performance.dto.CreatePerformanceScheduleRequest
import com.github.copyinaction.performance.dto.PerformanceScheduleResponse
import com.github.copyinaction.performance.dto.UpdatePerformanceScheduleRequest // 추가
import com.github.copyinaction.performance.service.PerformanceScheduleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter // 추가
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@Tag(name = "performance-schedule", description = "관리자용 공연 회차 CRUD API")
@RestController
@RequestMapping("/api/admin/performances") // RequestMapping 경로 변경
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
class PerformanceScheduleController(
    private val performanceScheduleService: PerformanceScheduleService
) {
    @Operation(summary = "공연 회차 생성", description = "특정 공연에 대한 새로운 회차와 티켓 가격 옵션을 생성합니다.\n\n**권한: ADMIN**")
    @PostMapping("/{performanceId}/schedules") // 경로 변경
    fun createSchedule(
        @Parameter(description = "공연 ID", required = true, example = "1") @PathVariable performanceId: Long,
        @Valid @RequestBody request: CreatePerformanceScheduleRequest
    ): ResponseEntity<PerformanceScheduleResponse> {
        val response = performanceScheduleService.createSchedule(performanceId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "단일 공연 회차 조회", description = "ID로 특정 공연 회차의 정보를 조회합니다.\n\n**권한: ADMIN**")
    @GetMapping("/schedules/{scheduleId}") // 새로운 경로
    fun getSchedule(
        @Parameter(description = "회차 ID", required = true, example = "1") @PathVariable scheduleId: Long
    ): ResponseEntity<PerformanceScheduleResponse> {
        val response = performanceScheduleService.getSchedule(scheduleId)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "특정 공연의 모든 회차 조회", description = "특정 공연에 대한 모든 회차 목록을 조회합니다.\n\n**권한: ADMIN**")
    @GetMapping("/{performanceId}/schedules") // 기존 경로 사용
    fun getAllSchedules(
        @Parameter(description = "공연 ID", required = true, example = "1") @PathVariable performanceId: Long
    ): ResponseEntity<List<PerformanceScheduleResponse>> {
        val responses = performanceScheduleService.getAllSchedules(performanceId)
        return ResponseEntity.ok(responses)
    }

    @Operation(summary = "공연 회차 수정", description = "특정 공연 회차의 정보를 수정합니다.\n\n**권한: ADMIN**")
    @PutMapping("/schedules/{scheduleId}") // 새로운 경로
    fun updateSchedule(
        @Parameter(description = "회차 ID", required = true, example = "1") @PathVariable scheduleId: Long,
        @Valid @RequestBody request: UpdatePerformanceScheduleRequest
    ): ResponseEntity<PerformanceScheduleResponse> {
        val response = performanceScheduleService.updateSchedule(scheduleId, request)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "공연 회차 삭제", description = "특정 공연 회차를 삭제합니다.\n\n**권한: ADMIN**")
    @DeleteMapping("/schedules/{scheduleId}") // 새로운 경로
    fun deleteSchedule(
        @Parameter(description = "회차 ID", required = true, example = "1") @PathVariable scheduleId: Long
    ): ResponseEntity<Unit> {
        performanceScheduleService.deleteSchedule(scheduleId)
        return ResponseEntity.noContent().build()
    }
}
