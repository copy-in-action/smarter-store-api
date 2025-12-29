package com.github.copyinaction.audit.service

import com.github.copyinaction.audit.domain.AuditAction
import com.github.copyinaction.audit.domain.AuditCategory
import com.github.copyinaction.audit.domain.AuditLog
import com.github.copyinaction.audit.domain.AuditTargetType
import com.github.copyinaction.audit.dto.*
import com.github.copyinaction.audit.repository.AuditLogRepository
import com.github.copyinaction.auth.domain.Role
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import jakarta.persistence.criteria.Predicate
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.LocalDateTime

@Service
class AuditLogService(
    private val auditLogRepository: AuditLogRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 비동기로 감사 로그 저장
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveAsync(
        userId: Long,
        userEmail: String?,
        userRole: Role?,
        action: AuditAction,
        targetType: AuditTargetType?,
        targetId: String?,
        description: String?,
        requestPath: String?,
        requestMethod: String?,
        requestBody: String?,
        responseStatus: Int?,
        ipAddress: String?,
        userAgent: String?
    ) {
        try {
            val auditLog = AuditLog.create(
                userId = userId,
                userEmail = userEmail,
                userRole = userRole,
                action = action,
                targetType = targetType,
                targetId = targetId,
                description = description,
                requestPath = requestPath,
                requestMethod = requestMethod,
                requestBody = maskSensitiveData(requestBody),
                responseStatus = responseStatus,
                ipAddress = ipAddress,
                userAgent = userAgent
            )
            auditLogRepository.save(auditLog)
            log.debug("Audit log saved: action={}, userId={}, targetId={}", action, userId, targetId)
        } catch (e: Exception) {
            log.error("Failed to save audit log: action={}, userId={}", action, userId, e)
        }
    }

    /**
     * 인증 이벤트 저장 (로그인 성공/실패 등)
     * IP와 User-Agent는 자동으로 현재 요청에서 추출
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveAuthEvent(
        action: AuditAction,
        email: String?,
        success: Boolean,
        userId: Long? = null,
        userRole: Role? = null
    ) {
        try {
            val request = getHttpServletRequest()
            val auditLog = AuditLog(
                userId = userId ?: 0,
                userEmail = email,
                userRole = userRole,
                action = action,
                category = action.category,
                description = if (success) "성공" else "실패",
                ipAddress = getClientIp(request),
                userAgent = request?.getHeader("User-Agent")
            )
            auditLogRepository.save(auditLog)
            log.debug("Auth event saved: action={}, email={}, success={}", action, email, success)
        } catch (e: Exception) {
            log.error("Failed to save auth event: action={}, email={}", action, email, e)
        }
    }

    // ==================== Private Helper Methods ====================

    private fun getHttpServletRequest(): HttpServletRequest? {
        return try {
            (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        } catch (e: Exception) {
            null
        }
    }

    private fun getClientIp(request: HttpServletRequest?): String? {
        if (request == null) return null

        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            return xForwardedFor.split(",").firstOrNull()?.trim()
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp
        }

        return request.remoteAddr
    }

    /**
     * 민감 정보 마스킹
     */
    private fun maskSensitiveData(requestBody: String?): String? {
        if (requestBody.isNullOrBlank()) return null

        return SENSITIVE_DATA_REGEX.replace(requestBody) { matchResult ->
            val key = matchResult.groupValues[1]
            "\"$key\":\"***\""
        }
    }

    companion object {
        private val SENSITIVE_KEYS = listOf(
            "password", "newPassword", "currentPassword",
            "cardNumber", "cvv", "securityCode"
        ).joinToString("|")

        private val SENSITIVE_DATA_REGEX = Regex("\"($SENSITIVE_KEYS)\"\\s*:\\s*\"[^\"]*\"")
    }

    // ==================== 조회 API ====================

    /**
     * 감사 로그 목록 조회 (필터 지원 - Specification 사용)
     */
    @Transactional(readOnly = true)
    fun getAuditLogs(
        userId: Long?,
        action: AuditAction?,
        category: AuditCategory?,
        targetType: AuditTargetType?,
        targetId: String?,
        from: LocalDateTime?,
        to: LocalDateTime?,
        pageable: Pageable
    ): Page<AuditLogResponse> {
        val spec = Specification<AuditLog> { root, _, cb ->
            val predicates = mutableListOf<Predicate>()

            userId?.let { predicates.add(cb.equal(root.get<Long>("userId"), it)) }
            action?.let { predicates.add(cb.equal(root.get<AuditAction>("action"), it)) }
            category?.let { predicates.add(cb.equal(root.get<AuditCategory>("category"), it)) }
            targetType?.let { predicates.add(cb.equal(root.get<AuditTargetType>("targetType"), it)) }
            targetId?.let { predicates.add(cb.equal(root.get<String>("targetId"), it)) }
            from?.let { predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), it)) }
            to?.let { predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), it)) }

            cb.and(*predicates.toTypedArray())
        }

        return auditLogRepository.findAll(spec, pageable)
            .map { AuditLogResponse.from(it) }
    }

    /**
     * 감사 로그 상세 조회
     */
    @Transactional(readOnly = true)
    fun getAuditLogDetail(id: Long): AuditLogDetailResponse {
        val auditLog = auditLogRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.AUDIT_LOG_NOT_FOUND) }
        return AuditLogDetailResponse.from(auditLog)
    }

    /**
     * 특정 사용자의 감사 로그 조회
     */
    @Transactional(readOnly = true)
    fun getAuditLogsByUserId(userId: Long, pageable: Pageable): Page<AuditLogResponse> {
        return auditLogRepository.findByUserId(userId, pageable)
            .map { AuditLogResponse.from(it) }
    }

    /**
     * 감사 로그 통계
     */
    @Transactional(readOnly = true)
    fun getAuditLogStats(from: LocalDateTime, to: LocalDateTime): AuditLogStatsResponse {
        val totalCount = auditLogRepository.countByCreatedAtBetween(from, to)

        val categoryStats = auditLogRepository.countByCategoryBetween(from, to)
            .associate {
                val category = it[0] as AuditCategory
                val count = it[1] as Long
                category to count
            }

        val actionStats = auditLogRepository.countByActionBetween(from, to)
            .take(10)
            .map {
                val action = it[0] as AuditAction
                val count = it[1] as Long
                ActionCount(
                    action = action,
                    description = action.description,
                    count = count
                )
            }

        return AuditLogStatsResponse(
            period = PeriodInfo(from = from, to = to),
            totalCount = totalCount,
            byCategory = categoryStats,
            byAction = actionStats
        )
    }
}
