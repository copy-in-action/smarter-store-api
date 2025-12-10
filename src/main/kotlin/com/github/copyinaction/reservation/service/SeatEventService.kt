package com.github.copyinaction.reservation.service

import com.github.copyinaction.reservation.domain.SeatStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class SeatEventService {

    private val logger = LoggerFactory.getLogger(javaClass)

    // 회차별 이벤트 Sink 관리
    private val scheduleSinks = ConcurrentHashMap<Long, Sinks.Many<SeatStatusEvent>>()

    /**
     * 회차별 좌석 상태 변경 이벤트 스트림을 구독합니다.
     */
    fun getSeatEventStream(scheduleId: Long): Flux<SeatStatusEvent> {
        val sink = scheduleSinks.computeIfAbsent(scheduleId) {
            Sinks.many().multicast().onBackpressureBuffer()
        }

        return sink.asFlux()
            .doOnSubscribe {
                logger.debug("Client subscribed to schedule $scheduleId seat events")
            }
            .doOnCancel {
                logger.debug("Client unsubscribed from schedule $scheduleId seat events")
            }
            // 30초마다 하트비트 전송
            .mergeWith(
                Flux.interval(Duration.ofSeconds(30))
                    .map {
                        SeatStatusEvent(
                            scheduleSeatId = -1,
                            status = null,
                            timestamp = LocalDateTime.now(),
                            isHeartbeat = true
                        )
                    }
            )
    }

    /**
     * 좌석 상태 변경 이벤트를 발행합니다.
     */
    fun publishSeatStatusChange(scheduleId: Long, scheduleSeatId: Long, newStatus: SeatStatus) {
        val sink = scheduleSinks[scheduleId]
        if (sink != null) {
            val event = SeatStatusEvent(
                scheduleSeatId = scheduleSeatId,
                status = newStatus,
                timestamp = LocalDateTime.now()
            )
            sink.tryEmitNext(event)
            logger.debug("Published seat status event: scheduleId=$scheduleId, seatId=$scheduleSeatId, status=$newStatus")
        }
    }

    /**
     * 다중 좌석 상태 변경 이벤트를 발행합니다.
     */
    fun publishMultipleSeatStatusChanges(scheduleId: Long, seatStatusMap: Map<Long, SeatStatus>) {
        val sink = scheduleSinks[scheduleId]
        if (sink != null) {
            seatStatusMap.forEach { (seatId, status) ->
                val event = SeatStatusEvent(
                    scheduleSeatId = seatId,
                    status = status,
                    timestamp = LocalDateTime.now()
                )
                sink.tryEmitNext(event)
            }
            logger.debug("Published ${seatStatusMap.size} seat status events for schedule $scheduleId")
        }
    }

    /**
     * 회차의 이벤트 스트림을 정리합니다. (메모리 관리용)
     */
    fun cleanupScheduleStream(scheduleId: Long) {
        scheduleSinks.remove(scheduleId)?.tryEmitComplete()
        logger.debug("Cleaned up seat event stream for schedule $scheduleId")
    }
}

data class SeatStatusEvent(
    val scheduleSeatId: Long,
    val status: SeatStatus?,
    val timestamp: LocalDateTime,
    val isHeartbeat: Boolean = false
)
