package com.github.copyinaction.reservation.controller

import com.github.copyinaction.common.exception.ErrorResponse
import com.github.copyinaction.reservation.dto.CreateScheduleTicketStockRequest
import com.github.copyinaction.reservation.dto.ScheduleTicketStockResponse
import com.github.copyinaction.reservation.dto.UpdateScheduleTicketStockRequest
import com.github.copyinaction.reservation.service.ScheduleTicketStockService
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
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.net.URI

@Tag(name = "schedule-ticket-stocks", description = "회차별 좌석등급 재고 API")
@RestController
@RequestMapping("/api/schedule-ticket-stocks")
class ScheduleTicketStockController(
    private val scheduleTicketStockService: ScheduleTicketStockService
) {

    @Operation(summary = "재고 생성", description = "회차별 좌석등급 재고를 생성합니다.\n\n**권한: ADMIN**")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "재고 생성 성공"),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 입력 값 또는 이미 존재하는 재고",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "회차 또는 좌석등급을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    fun createStock(
        @Valid @RequestBody request: CreateScheduleTicketStockRequest
    ): ResponseEntity<ScheduleTicketStockResponse> {
        val stock = scheduleTicketStockService.createStock(request)
        val location = URI.create("/api/schedule-ticket-stocks/${stock.id}")
        return ResponseEntity.created(location).body(stock)
    }

    @Operation(summary = "재고 조회", description = "회차별 좌석등급 재고를 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "재고 조회 성공"),
        ApiResponse(
            responseCode = "404",
            description = "재고를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping("/{id}")
    fun getStock(
        @Parameter(description = "재고 ID") @PathVariable id: Long
    ): ResponseEntity<ScheduleTicketStockResponse> {
        val stock = scheduleTicketStockService.getStock(id)
        return ResponseEntity.ok(stock)
    }

    @Operation(summary = "회차별 재고 목록 조회", description = "특정 회차의 모든 좌석등급 재고를 조회합니다.")
    @GetMapping("/schedule/{scheduleId}")
    fun getStocksBySchedule(
        @Parameter(description = "회차 ID") @PathVariable scheduleId: Long
    ): ResponseEntity<List<ScheduleTicketStockResponse>> {
        val stocks = scheduleTicketStockService.getStocksByScheduleId(scheduleId)
        return ResponseEntity.ok(stocks)
    }

    @Operation(summary = "공연별 재고 목록 조회", description = "특정 공연의 모든 회차/좌석등급 재고를 조회합니다.")
    @GetMapping("/performance/{performanceId}")
    fun getStocksByPerformance(
        @Parameter(description = "공연 ID") @PathVariable performanceId: Long
    ): ResponseEntity<List<ScheduleTicketStockResponse>> {
        val stocks = scheduleTicketStockService.getStocksByPerformanceId(performanceId)
        return ResponseEntity.ok(stocks)
    }

    @Operation(summary = "재고 수정", description = "회차별 좌석등급 재고를 수정합니다.\n\n**권한: ADMIN**")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "재고 수정 성공"),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 입력 값",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "재고를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    fun updateStock(
        @Parameter(description = "재고 ID") @PathVariable id: Long,
        @Valid @RequestBody request: UpdateScheduleTicketStockRequest
    ): ResponseEntity<ScheduleTicketStockResponse> {
        val stock = scheduleTicketStockService.updateStock(id, request)
        return ResponseEntity.ok(stock)
    }

    @Operation(summary = "재고 삭제", description = "회차별 좌석등급 재고를 삭제합니다.\n\n**권한: ADMIN**")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "재고 삭제 성공"),
        ApiResponse(
            responseCode = "404",
            description = "재고를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    fun deleteStock(
        @Parameter(description = "재고 ID") @PathVariable id: Long
    ): ResponseEntity<Unit> {
        scheduleTicketStockService.deleteStock(id)
        return ResponseEntity.noContent().build()
    }
}
