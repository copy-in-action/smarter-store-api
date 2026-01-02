package com.github.copyinaction.venue.service

import com.github.copyinaction.performance.service.TicketOptionSyncService
import com.github.copyinaction.venue.domain.SeatingChartUpdatedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class VenueEventHandler(
    private val ticketOptionSyncService: TicketOptionSyncService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener
    @Transactional
    fun handleSeatingChartUpdated(event: SeatingChartUpdatedEvent) {
        log.debug("이벤트 핸들러 - 좌석 배치도 업데이트 처리: venueId={}", event.venueId)
        
        // 해당 공연장의 모든 스케줄의 TicketOption.totalQuantity 동기화
        ticketOptionSyncService.syncTotalQuantityByVenue(event.venueId, event.seatingChartJson)
    }
}
