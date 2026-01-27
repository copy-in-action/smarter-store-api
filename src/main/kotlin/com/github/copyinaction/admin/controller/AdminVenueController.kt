package com.github.copyinaction.admin.controller

import com.github.copyinaction.audit.annotation.Auditable
import com.github.copyinaction.audit.domain.AuditAction
import com.github.copyinaction.audit.domain.AuditTargetType
import com.github.copyinaction.common.exception.ErrorResponse
import com.github.copyinaction.venue.dto.CreateVenueRequest
import com.github.copyinaction.venue.dto.SeatingChartRequest
import com.github.copyinaction.venue.dto.SeatingChartResponse
import com.github.copyinaction.venue.dto.UpdateVenueRequest
import com.github.copyinaction.venue.dto.VenueResponse
import com.github.copyinaction.venue.service.VenueService
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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@Tag(name = "admin-venue", description = "관리자용 공연장 API - 공연장 생성/수정/삭제")
@RestController
@RequestMapping("/api/admin/venues")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
class AdminVenueController(
    private val venueService: VenueService
) {

    @Operation(summary = "공연장 생성", description = "새로운 공연장 정보를 생성합니다.\n\n**권한: ADMIN**\n\n**[Audit Log]** 이 작업은 감사 로그에 기록됩니다.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "공연장 생성 성공"),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 입력 값",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "접근 권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping
    @Auditable(
        action = AuditAction.VENUE_CREATE,
        targetType = AuditTargetType.VENUE,
        includeRequestBody = true
    )
    fun createVenue(@Valid @RequestBody request: CreateVenueRequest): ResponseEntity<VenueResponse> {
        val venue = venueService.createVenue(request)
        val location = URI.create("/api/admin/venues/${venue.id}")
        return ResponseEntity.created(location).body(venue)
    }

    @Operation(summary = "공연장 정보 수정", description = "특정 공연장의 정보를 수정합니다.\n\n**권한: ADMIN**\n\n**[Audit Log]** 이 작업은 감사 로그에 기록됩니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "공연장 정보 수정 성공"),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 입력 값",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "공연장을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "접근 권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PutMapping("/{id}")
    @Auditable(
        action = AuditAction.VENUE_UPDATE,
        targetType = AuditTargetType.VENUE,
        targetIdParam = "id",
        includeRequestBody = true
    )
    fun updateVenue(
        @Parameter(description = "수정할 공연장의 ID", required = true, example = "1") @PathVariable id: Long,
        @Valid @RequestBody request: UpdateVenueRequest
    ): ResponseEntity<VenueResponse> {
        val venue = venueService.updateVenue(id, request)
        return ResponseEntity.ok(venue)
    }

    @Operation(summary = "공연장 삭제", description = "특정 공연장을 삭제합니다.\n\n**권한: ADMIN**\n\n**[Audit Log]** 이 작업은 감사 로그에 기록됩니다.")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "공연장 삭제 성공"),
        ApiResponse(
            responseCode = "404",
            description = "공연장을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "접근 권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @DeleteMapping("/{id}")
    @Auditable(
        action = AuditAction.VENUE_DELETE,
        targetType = AuditTargetType.VENUE,
        targetIdParam = "id"
    )
    fun deleteVenue(@Parameter(description = "삭제할 공연장의 ID", required = true, example = "1") @PathVariable id: Long): ResponseEntity<Unit> {
        venueService.deleteVenue(id)
        return ResponseEntity.noContent().build()
    }

    // === 좌석 배치도 API ===

    @Operation(
        summary = "좌석 배치도 저장/수정",
        description = "공연장의 좌석 배치도 JSON과 등급별 좌석 수를 함께 저장하거나 수정합니다.\n\n" +
            "seatCapacities가 전달되면 기존 좌석 수를 삭제하고 새로 저장합니다.\n\n**권한: ADMIN**\n\n**[Audit Log]** 이 작업은 감사 로그에 기록됩니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "좌석 배치도 저장 성공"),
        ApiResponse(
            responseCode = "404",
            description = "공연장을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "접근 권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PutMapping("/{id}/seating-chart")
    @Auditable(
        action = AuditAction.VENUE_SEATING_CHART_UPDATE,
        targetType = AuditTargetType.VENUE,
        targetIdParam = "id",
        includeRequestBody = true
    )
    fun updateSeatingChart(
        @Parameter(description = "공연장 ID", required = true, example = "1") @PathVariable id: Long,
        @Valid @RequestBody request: SeatingChartRequest
    ): ResponseEntity<SeatingChartResponse> {
        val seatingChart = venueService.updateSeatingChart(id, request)
        return ResponseEntity.ok(seatingChart)
    }
}
