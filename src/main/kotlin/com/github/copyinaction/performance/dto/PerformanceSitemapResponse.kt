package com.github.copyinaction.performance.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "사이트맵 생성용 공연 정보 응답")
data class PerformanceSitemapResponse(
    @Schema(description = "공연 ID", example = "1")
    val id: Long,

    @Schema(description = "마지막 수정 일시", example = "2026-01-07T12:00:00")
    val lastModifiedAt: LocalDateTime?
)
