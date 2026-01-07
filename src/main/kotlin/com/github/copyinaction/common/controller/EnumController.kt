package com.github.copyinaction.common.controller

import com.github.copyinaction.audit.domain.AuditAction
import com.github.copyinaction.audit.domain.AuditCategory
import com.github.copyinaction.audit.domain.AuditTargetType
import com.github.copyinaction.auth.domain.Role
import com.github.copyinaction.booking.domain.BookingStatus
import com.github.copyinaction.common.dto.EnumResponse
import com.github.copyinaction.notice.domain.NoticeCategory
import com.github.copyinaction.seat.domain.SeatStatus
import com.github.copyinaction.seat.dto.SeatEventAction
import com.github.copyinaction.venue.domain.SeatGrade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/enums")
@Tag(name = "enums", description = "프론트엔드에서 사용하는 공통 Enum 코드 조회 API")
class EnumController {

    @GetMapping
    @Operation(summary = "전체 Enum 목록", description = "모든 공통 Enum을 한번에 조회합니다. 앱 초기화 시 호출하여 캐싱해서 사용하세요.\n\n**권한: 누구나**")
    fun getAllEnums(): Map<String, List<EnumResponse>> {
        return mapOf(
            "seatGrades" to SeatGrade.entries.map { EnumResponse(it.name, it.description) },
            "bookingStatuses" to BookingStatus.entries.map { EnumResponse(it.name, it.description) },
            "seatStatuses" to SeatStatus.entries.map { EnumResponse(it.name, it.description) },
            "noticeCategories" to NoticeCategory.entries.map { EnumResponse(it.name, it.description) },
            "roles" to Role.entries.map { EnumResponse(it.name, it.description) },
            "auditCategories" to AuditCategory.entries.map { EnumResponse(it.name, it.description) },
            "auditActions" to AuditAction.entries.map { EnumResponse(it.name, it.description) },
            "auditTargetTypes" to AuditTargetType.entries.map { EnumResponse(it.name, it.description) },
            "seatEventActions" to SeatEventAction.entries.map { EnumResponse(it.name, it.description) }
        )
    }
}
