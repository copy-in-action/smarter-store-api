package com.github.copyinaction.performance.controller

import com.github.copyinaction.common.exception.ErrorResponse
import com.github.copyinaction.performance.dto.AvailableScheduleResponse
import com.github.copyinaction.performance.dto.PerformanceResponse
import com.github.copyinaction.performance.service.PerformanceScheduleService
import com.github.copyinaction.performance.service.PerformanceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime

@Tag(name = "performance", description = "공연 API - 공연 조회 API")
@RestController
@RequestMapping("/api/performances")
class PerformanceController(
    private val performanceService: PerformanceService,
    private val performanceScheduleService: PerformanceScheduleService
) {
    @Operation(summary = "단일 공연 조회", description = "ID로 특정 공연의 정보를 조회합니다.\n\n**권한: 누구나**")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "공연 조회 성공"),
        ApiResponse(
            responseCode = "404",
            description = "공연을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping("/{id}")
    fun getPerformance(@Parameter(description = "조회할 공연의 ID", required = true, example = "1") @PathVariable id: Long): ResponseEntity<PerformanceResponse> {
        val performance = performanceService.getPerformance(id)
        return ResponseEntity.ok(performance)
    }

    @Operation(summary = "모든 공연 조회", description = "모든 공연 목록을 조회합니다.\n\n**권한: 누구나**")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "공연 목록 조회 성공")
    )
    @GetMapping
    fun getAllPerformances(): ResponseEntity<List<PerformanceResponse>> {
        val performances = performanceService.getAllPerformances()
        return ResponseEntity.ok(performances)
    }

    // === 사용자용 회차 조회 API ===

    @Operation(
        summary = "예매 가능 회차 날짜 목록 조회",
        description = "해당 공연의 예매 가능한 회차 날짜 목록을 조회합니다.\n\n" +
            "- 티켓 판매가 시작되고 공연이 아직 시작하지 않은 회차만 반환\n\n" +
            "**권한: 누구나**"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(
            responseCode = "404",
            description = "공연을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping("/{id}/schedules/dates")
    fun getAvailableScheduleDates(
        @Parameter(description = "공연 ID", required = true, example = "1") @PathVariable id: Long
    ): ResponseEntity<List<LocalDateTime>> {
        val dates = performanceScheduleService.getAvailableDates(id)
        return ResponseEntity.ok(dates)
    }

    @Operation(
        summary = "특정 날짜의 예매 가능 회차 목록 조회",
        description = "해당 공연의 특정 날짜에 예매 가능한 회차 목록을 조회합니다.\n\n" +
            "- 공연시간 내림차순 정렬\n" +
            "- 각 좌석 등급의 잔여석 수 포함\n\n" +
            "**권한: 누구나**"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(
            responseCode = "404",
            description = "공연을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping("/{id}/schedules")
    fun getAvailableSchedulesByDate(
        @Parameter(description = "공연 ID", required = true, example = "1") @PathVariable id: Long,
        @Parameter(description = "조회할 날짜 (yyyy-MM-dd)", required = true, example = "2026-01-02")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<List<AvailableScheduleResponse>> {
        val schedules = performanceScheduleService.getAvailableSchedulesByDate(id, date)
        return ResponseEntity.ok(schedules)
    }
}