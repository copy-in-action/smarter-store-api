package com.github.copyinaction.audit.annotation

import com.github.copyinaction.audit.domain.AuditAction
import com.github.copyinaction.audit.domain.AuditTargetType

/**
 * 감사 로그 기록 대상 메서드 마킹 어노테이션
 *
 * @param action 기록할 액션 타입
 * @param targetType 대상 타입 (기본값: USER)
 * @param targetIdParam 대상 ID를 추출할 파라미터명 (예: "bookingId")
 * @param description 커스텀 설명 (비어있으면 action의 기본 설명 사용)
 * @param includeRequestBody 요청 본문 포함 여부 (민감정보 마스킹됨)
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Auditable(
    val action: AuditAction,
    val targetType: AuditTargetType = AuditTargetType.USER,
    val targetIdParam: String = "",
    val description: String = "",
    val includeRequestBody: Boolean = false
)
