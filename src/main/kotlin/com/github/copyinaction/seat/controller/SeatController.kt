package com.github.copyinaction.seat.controller

import com.github.copyinaction.common.exception.ErrorResponse
import com.github.copyinaction.seat.dto.ScheduleSeatStatusResponse
import com.github.copyinaction.seat.service.SeatService
import com.github.copyinaction.seat.service.SseService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Tag(name = "seat", description = "좌석 API - 좌석 상태 조회 및 실시간 구독")
@RestController
@RequestMapping("/api/schedules/{scheduleId}")
class SeatController(
    private val seatService: SeatService,
    private val sseService: SseService
) {

    @Operation(
        summary = "회차별 좌석 상태 조회",
        description = "특정 회차의 좌석 상태 목록을 조회합니다. PENDING(점유 중), RESERVED(예약 완료) 상태인 좌석만 반환합니다.\n\n**권한: 인증 불필요**"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "좌석 상태 조회 성공"),
        ApiResponse(
            responseCode = "404",
            description = "회차를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping("/seat-status")
    fun getSeatStatus(
        @Parameter(description = "회차 ID", required = true, example = "1")
        @PathVariable scheduleId: Long
    ): ResponseEntity<ScheduleSeatStatusResponse> {
        val seatStatus = seatService.getSeatStatus(scheduleId)
        return ResponseEntity.ok(seatStatus)
    }

    @Operation(
        summary = "좌석 상태 실시간 구독 (SSE)",
        description = """
            SSE(Server-Sent Events)를 통해 좌석 상태 변경을 실시간으로 수신합니다.

            **이벤트 타입:**
            - `connect`: 연결 성공
            - `seat-update`: 좌석 상태 변경 (OCCUPIED/RELEASED/CONFIRMED)
            - `heartbeat`: 연결 유지 (45초마다)

            **권한: 인증 불필요**
        """
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "SSE 스트림 연결 성공")
    )
    @GetMapping("/seats/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribeSeatEvents(
        @Parameter(description = "회차 ID", required = true, example = "1")
        @PathVariable scheduleId: Long
    ): SseEmitter {
        return sseService.subscribe(scheduleId)
    }
}
