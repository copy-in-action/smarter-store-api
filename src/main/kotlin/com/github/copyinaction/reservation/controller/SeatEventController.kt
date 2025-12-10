package com.github.copyinaction.reservation.controller

import com.github.copyinaction.reservation.service.SeatEventService
import com.github.copyinaction.reservation.service.SeatStatusEvent
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@Tag(name = "seat-events", description = "좌석 실시간 이벤트 API - SSE 기반 실시간 좌석 상태 업데이트")
@RestController
@RequestMapping("/api/schedules/{scheduleId}/seats")
class SeatEventController(
    private val seatEventService: SeatEventService
) {

    @Operation(
        summary = "좌석 상태 실시간 업데이트 (SSE)",
        description = """
            Server-Sent Events를 통해 실시간 좌석 상태 변경을 수신합니다.

            **사용 방법:**
            ```javascript
            const eventSource = new EventSource('/api/schedules/1/seats/events');

            eventSource.onmessage = (event) => {
                const data = JSON.parse(event.data);
                if (!data.isHeartbeat) {
                    // 좌석 상태 업데이트 처리
                    console.log('Seat', data.scheduleSeatId, 'changed to', data.status);
                }
            };

            eventSource.onerror = (error) => {
                console.error('SSE connection error:', error);
                eventSource.close();
            };
            ```

            **이벤트 데이터 형식:**
            - `scheduleSeatId`: 좌석 ID (-1이면 하트비트)
            - `status`: 새 상태 (AVAILABLE, HELD, RESERVED)
            - `timestamp`: 이벤트 발생 시간
            - `isHeartbeat`: 하트비트 여부 (30초마다 전송)
        """
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "SSE 스트림 연결 성공")
    )
    @GetMapping("/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamSeatEvents(
        @Parameter(description = "회차 ID") @PathVariable scheduleId: Long
    ): Flux<SeatStatusEvent> {
        return seatEventService.getSeatEventStream(scheduleId)
    }
}
