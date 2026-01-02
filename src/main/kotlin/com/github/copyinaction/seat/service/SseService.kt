package com.github.copyinaction.seat.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.copyinaction.seat.dto.SeatEventMessage
import com.github.copyinaction.seat.domain.SeatPosition
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Service
class SseService(
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val SSE_TIMEOUT = 5 * 60 * 1000L // 5분
        private const val HEARTBEAT_INTERVAL = 45 * 1000L // 45초
    }

    private val emitters = ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>()

    /**
     * SSE 연결 구독
     */
    fun subscribe(scheduleId: Long): SseEmitter {
        val emitter = SseEmitter(SSE_TIMEOUT)

        emitters.computeIfAbsent(scheduleId) { CopyOnWriteArrayList() }.add(emitter)

        emitter.onCompletion { removeEmitter(scheduleId, emitter) }
        emitter.onTimeout { removeEmitter(scheduleId, emitter) }
        emitter.onError { removeEmitter(scheduleId, emitter) }

        // 연결 성공 시 초기 이벤트 전송 (연결 확인용)
        try {
            emitter.send(
                SseEmitter.event()
                    .name("connect")
                    .data("connected")
            )
        } catch (e: IOException) {
            log.warn("SSE 초기 이벤트 전송 실패 - scheduleId: {}", scheduleId)
            removeEmitter(scheduleId, emitter)
        }

        log.debug("SSE 구독 등록 - scheduleId: {}, 현재 연결 수: {}", scheduleId, emitters[scheduleId]?.size ?: 0)

        return emitter
    }

    /**
     * 좌석 점유 이벤트 전송
     */
    fun sendOccupied(scheduleId: Long, seats: List<SeatPosition>) {
        sendEvent(scheduleId, SeatEventMessage.occupied(seats))
    }

    /**
     * 좌석 해제 이벤트 전송
     */
    fun sendReleased(scheduleId: Long, seats: List<SeatPosition>) {
        sendEvent(scheduleId, SeatEventMessage.released(seats))
    }

    /**
     * 좌석 확정 이벤트 전송
     */
    fun sendConfirmed(scheduleId: Long, seats: List<SeatPosition>) {
        sendEvent(scheduleId, SeatEventMessage.confirmed(seats))
    }

    /**
     * 이벤트 전송 (내부)
     */
    private fun sendEvent(scheduleId: Long, message: SeatEventMessage) {
        val scheduleEmitters = emitters[scheduleId] ?: return

        val deadEmitters = mutableListOf<SseEmitter>()
        val jsonData = objectMapper.writeValueAsString(message)

        scheduleEmitters.forEach { emitter ->
            try {
                emitter.send(
                    SseEmitter.event()
                        .name("seat-update")
                        .data(jsonData)
                )
            } catch (e: IOException) {
                log.debug("SSE 이벤트 전송 실패 - scheduleId: {}", scheduleId)
                deadEmitters.add(emitter)
            }
        }

        deadEmitters.forEach { removeEmitter(scheduleId, it) }

        log.debug("SSE 이벤트 전송 - scheduleId: {}, action: {}, 대상 수: {}",
            scheduleId, message.action, scheduleEmitters.size - deadEmitters.size)
    }

    /**
     * Heartbeat 전송 (45초마다)
     * - 연결 유지 및 503 에러 방지
     */
    @Scheduled(fixedRate = HEARTBEAT_INTERVAL)
    fun sendHeartbeat() {
        emitters.forEach { (scheduleId, scheduleEmitters) ->
            val deadEmitters = mutableListOf<SseEmitter>()

            scheduleEmitters.forEach { emitter ->
                try {
                    emitter.send(
                        SseEmitter.event()
                            .name("heartbeat")
                            .data("")
                    )
                } catch (e: IOException) {
                    deadEmitters.add(emitter)
                }
            }

            deadEmitters.forEach { removeEmitter(scheduleId, it) }
        }
    }

    /**
     * Emitter 제거
     */
    private fun removeEmitter(scheduleId: Long, emitter: SseEmitter) {
        emitters[scheduleId]?.remove(emitter)

        // 연결이 없으면 Map에서 제거
        emitters[scheduleId]?.let {
            if (it.isEmpty()) {
                emitters.remove(scheduleId)
            }
        }

        log.debug("SSE 연결 해제 - scheduleId: {}, 남은 연결 수: {}", scheduleId, emitters[scheduleId]?.size ?: 0)
    }

    /**
     * 특정 회차의 연결 수 조회 (테스트/모니터링용)
     */
    fun getConnectionCount(scheduleId: Long): Int {
        return emitters[scheduleId]?.size ?: 0
    }
}
