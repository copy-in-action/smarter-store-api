package com.github.copyinaction.venue.domain

/**
 * 좌석 등급
 * 공연장 및 티켓에서 공통으로 사용되는 좌석 등급 Enum
 */
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "좌석 등급 (VIP: VIP석, R: R석, S: S석, A: A석, B: B석)")
enum class SeatGrade(val description: String) {
    VIP("VIP석"),
    R("R석"),
    S("S석"),
    A("A석"),
    B("B석")
}
