package com.github.copyinaction.audit.dto

import com.github.copyinaction.audit.domain.AuditAction
import com.github.copyinaction.audit.domain.AuditCategory
import com.github.copyinaction.audit.domain.AuditLog
import com.github.copyinaction.audit.domain.AuditTargetType
import com.github.copyinaction.auth.domain.Role
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "감사 로그 응답")
data class AuditLogResponse(
    @Schema(description = "로그 ID")
    val id: Long,

    @Schema(description = "사용자 ID")
    val userId: Long,

    @Schema(description = "사용자 이메일")
    val userEmail: String?,

    @Schema(description = "사용자 역할")
    val userRole: Role?,

    @Schema(description = "액션")
    val action: AuditAction,

    @Schema(description = "액션 설명")
    val actionDescription: String,

    @Schema(description = "카테고리")
    val category: AuditCategory,

    @Schema(description = "대상 타입")
    val targetType: AuditTargetType?,

    @Schema(description = "대상 ID")
    val targetId: String?,

    @Schema(description = "설명")
    val description: String?,

    @Schema(description = "요청 경로")
    val requestPath: String?,

    @Schema(description = "HTTP 메서드")
    val requestMethod: String?,

    @Schema(description = "응답 상태 코드")
    val responseStatus: Int?,

    @Schema(description = "응답 상태 설명 (관리자용)", example = "성공")
    val responseStatusDescription: String?,

    @Schema(description = "IP 주소")
    val ipAddress: String?,

    @Schema(description = "생성 일시")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(auditLog: AuditLog): AuditLogResponse {
            return AuditLogResponse(
                id = auditLog.id,
                userId = auditLog.userId,
                userEmail = maskEmail(auditLog.userEmail),
                userRole = auditLog.userRole,
                action = auditLog.action,
                actionDescription = auditLog.action.description,
                category = auditLog.category,
                targetType = auditLog.targetType,
                targetId = auditLog.targetId,
                description = auditLog.description,
                requestPath = auditLog.requestPath,
                requestMethod = auditLog.requestMethod,
                responseStatus = auditLog.responseStatus,
                responseStatusDescription = getStatusDescription(auditLog.responseStatus),
                ipAddress = auditLog.ipAddress,
                createdAt = auditLog.createdAt
            )
        }
    }
}

@Schema(description = "감사 로그 상세 응답 (요청 본문 포함)")
data class AuditLogDetailResponse(
    @Schema(description = "로그 ID")
    val id: Long,

    @Schema(description = "사용자 ID")
    val userId: Long,

    @Schema(description = "사용자 이메일")
    val userEmail: String?,

    @Schema(description = "사용자 역할")
    val userRole: Role?,

    @Schema(description = "액션")
    val action: AuditAction,

    @Schema(description = "액션 설명")
    val actionDescription: String,

    @Schema(description = "카테고리")
    val category: AuditCategory,

    @Schema(description = "대상 타입")
    val targetType: AuditTargetType?,

    @Schema(description = "대상 ID")
    val targetId: String?,

    @Schema(description = "설명")
    val description: String?,

    @Schema(description = "요청 경로")
    val requestPath: String?,

    @Schema(description = "HTTP 메서드")
    val requestMethod: String?,

    @Schema(description = "요청 본문 (민감정보 마스킹)")
    val requestBody: String?,

    @Schema(description = "응답 상태 코드")
    val responseStatus: Int?,

    @Schema(description = "응답 상태 설명 (관리자용)", example = "성공")
    val responseStatusDescription: String?,

    @Schema(description = "IP 주소")
    val ipAddress: String?,

    @Schema(description = "User-Agent")
    val userAgent: String?,

    @Schema(description = "생성 일시")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(auditLog: AuditLog): AuditLogDetailResponse {
            return AuditLogDetailResponse(
                id = auditLog.id,
                userId = auditLog.userId,
                userEmail = maskEmail(auditLog.userEmail),
                userRole = auditLog.userRole,
                action = auditLog.action,
                actionDescription = auditLog.action.description,
                category = auditLog.category,
                targetType = auditLog.targetType,
                targetId = auditLog.targetId,
                description = auditLog.description,
                requestPath = auditLog.requestPath,
                requestMethod = auditLog.requestMethod,
                requestBody = auditLog.requestBody,
                responseStatus = auditLog.responseStatus,
                responseStatusDescription = getStatusDescription(auditLog.responseStatus),
                ipAddress = auditLog.ipAddress,
                userAgent = auditLog.userAgent,
                createdAt = auditLog.createdAt
            )
        }
    }
}

private fun getStatusDescription(status: Int?): String {
    return when (status) {
        null -> "알 수 없음"
        in 200..299 -> "성공"
        400 -> "요청 오류 (Bad Request)"
        401 -> "인증 실패 (Unauthorized)"
        403 -> "권한 없음 (Forbidden)"
        404 -> "대상 없음 (Not Found)"
        409 -> "충돌 (Conflict)"
        in 400..499 -> "클라이언트 오류 ($status)"
        500 -> "서버 내부 오류"
        in 500..599 -> "서버 오류 ($status)"
        else -> "기타 ($status)"
    }
}

/**
 * 이메일 마스킹 (예: user@example.com -> u***r@example.com)
 */
private fun maskEmail(email: String?): String? {
    if (email.isNullOrBlank()) return null
    val parts = email.split("@")
    if (parts.size != 2) return email
    val localPart = parts[0]
    val domain = parts[1]
    val masked = when {
        localPart.length <= 2 -> "*".repeat(localPart.length)
        else -> "${localPart.first()}${"*".repeat(localPart.length - 2)}${localPart.last()}"
    }
    return "$masked@$domain"
}

@Schema(description = "감사 로그 통계 응답")
data class AuditLogStatsResponse(
    @Schema(description = "조회 기간")
    val period: PeriodInfo,

    @Schema(description = "전체 로그 수")
    val totalCount: Long,

    @Schema(description = "카테고리별 통계")
    val byCategory: Map<AuditCategory, Long>,

    @Schema(description = "액션별 통계 (상위 10개)")
    val byAction: List<ActionCount>
)

@Schema(description = "기간 정보")
data class PeriodInfo(
    @Schema(description = "시작 일시")
    val from: LocalDateTime,

    @Schema(description = "종료 일시")
    val to: LocalDateTime
)

@Schema(description = "액션별 통계")
data class ActionCount(
    @Schema(description = "액션")
    val action: AuditAction,

    @Schema(description = "설명")
    val description: String,

    @Schema(description = "건수")
    val count: Long
)