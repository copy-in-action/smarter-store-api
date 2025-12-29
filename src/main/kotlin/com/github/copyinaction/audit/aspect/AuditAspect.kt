package com.github.copyinaction.audit.aspect

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.copyinaction.audit.annotation.Auditable
import com.github.copyinaction.audit.service.AuditLogService
import com.github.copyinaction.auth.domain.Role
import com.github.copyinaction.auth.service.CustomUserDetails
import com.github.copyinaction.common.exception.CustomException
import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
@Component
class AuditAspect(
    private val auditLogService: AuditLogService,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Around("@annotation(auditable)")
    fun audit(joinPoint: ProceedingJoinPoint, auditable: Auditable): Any? {
        var responseStatus = 200

        try {
            val result = joinPoint.proceed()
            return result
        } catch (e: Exception) {
            responseStatus = when (e) {
                is CustomException -> 400
                else -> 500
            }
            throw e
        } finally {
            try {
                saveAuditLog(joinPoint, auditable, responseStatus)
            } catch (e: Exception) {
                log.error("Failed to save audit log", e)
            }
        }
    }

    private fun saveAuditLog(
        joinPoint: ProceedingJoinPoint,
        auditable: Auditable,
        responseStatus: Int
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
        val userDetails = authentication?.principal as? CustomUserDetails

        // 비인증 사용자는 기록하지 않음 (로그인/회원가입 등은 별도 처리)
        if (userDetails == null) {
            log.debug("Skipping audit log for unauthenticated request")
            return
        }

        val userId = userDetails.id
        val userEmail = userDetails.username
        val userRole = extractUserRole(userDetails)

        val targetId = extractTargetId(joinPoint, auditable.targetIdParam)
        val requestBody = if (auditable.includeRequestBody) extractRequestBody(joinPoint) else null

        val request = getHttpServletRequest()

        auditLogService.saveAsync(
            userId = userId,
            userEmail = userEmail,
            userRole = userRole,
            action = auditable.action,
            targetType = auditable.targetType,
            targetId = targetId,
            description = auditable.description.ifEmpty { null },
            requestPath = request?.requestURI,
            requestMethod = request?.method,
            requestBody = requestBody,
            responseStatus = responseStatus,
            ipAddress = getClientIp(request),
            userAgent = request?.getHeader("User-Agent")
        )
    }

    private fun extractUserRole(userDetails: CustomUserDetails): Role? {
        return userDetails.authorities
            .firstOrNull()
            ?.authority
            ?.removePrefix("ROLE_")
            ?.let {
                try {
                    Role.valueOf(it)
                } catch (e: Exception) {
                    null
                }
            }
    }

    private fun extractTargetId(joinPoint: ProceedingJoinPoint, targetIdParam: String): String? {
        if (targetIdParam.isBlank()) return null

        val signature = joinPoint.signature as MethodSignature
        val parameterNames = signature.parameterNames
        val args = joinPoint.args

        val paramIndex = parameterNames.indexOf(targetIdParam)
        if (paramIndex >= 0 && paramIndex < args.size) {
            return args[paramIndex]?.toString()
        }
        return null
    }

    private fun extractRequestBody(joinPoint: ProceedingJoinPoint): String? {
        val args = joinPoint.args
        if (args.isEmpty()) return null

        // @RequestBody로 전달된 객체 찾기 (보통 첫 번째 인자)
        return try {
            args.firstOrNull()?.let { objectMapper.writeValueAsString(it) }
        } catch (e: Exception) {
            log.warn("Failed to serialize request body", e)
            null
        }
    }

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
}
