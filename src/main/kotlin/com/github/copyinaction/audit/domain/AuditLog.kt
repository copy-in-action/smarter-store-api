package com.github.copyinaction.audit.domain

import com.github.copyinaction.auth.domain.Role
import jakarta.persistence.*
import org.hibernate.annotations.Comment
import java.time.LocalDateTime

@Entity
@Table(
    name = "audit_log",
    indexes = [
        Index(name = "idx_audit_log_user", columnList = "user_id"),
        Index(name = "idx_audit_log_action", columnList = "action"),
        Index(name = "idx_audit_log_created", columnList = "created_at"),
        Index(name = "idx_audit_log_target", columnList = "target_type, target_id"),
        Index(name = "idx_audit_log_category", columnList = "category")
    ]
)
@Comment("감사 로그")
class AuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    @Comment("사용자 ID (0: 미인증)")
    val userId: Long,

    @Column(name = "user_email", length = 100)
    @Comment("사용자 이메일")
    val userEmail: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", length = 20)
    @Comment("사용자 역할")
    val userRole: Role? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    @Comment("액션 타입")
    val action: AuditAction,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    @Comment("카테고리")
    val category: AuditCategory,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 30)
    @Comment("대상 타입")
    val targetType: AuditTargetType? = null,

    @Column(name = "target_id", length = 100)
    @Comment("대상 ID")
    val targetId: String? = null,

    @Column(name = "description", length = 500)
    @Comment("설명")
    val description: String? = null,

    @Column(name = "request_path", length = 200)
    @Comment("요청 경로")
    val requestPath: String? = null,

    @Column(name = "request_method", length = 10)
    @Comment("HTTP 메서드")
    val requestMethod: String? = null,

    @Column(name = "request_body", columnDefinition = "TEXT")
    @Comment("요청 본문 (민감정보 마스킹)")
    val requestBody: String? = null,

    @Column(name = "response_status")
    @Comment("응답 상태 코드")
    val responseStatus: Int? = null,

    @Column(name = "ip_address", length = 50)
    @Comment("클라이언트 IP")
    val ipAddress: String? = null,

    @Column(name = "user_agent", length = 500)
    @Comment("User-Agent")
    val userAgent: String? = null,

    @Column(name = "created_at", nullable = false)
    @Comment("생성 일시")
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun create(
            userId: Long,
            userEmail: String?,
            userRole: Role?,
            action: AuditAction,
            targetType: AuditTargetType? = null,
            targetId: String? = null,
            description: String? = null,
            requestPath: String? = null,
            requestMethod: String? = null,
            requestBody: String? = null,
            responseStatus: Int? = null,
            ipAddress: String? = null,
            userAgent: String? = null
        ): AuditLog {
            return AuditLog(
                userId = userId,
                userEmail = userEmail,
                userRole = userRole,
                action = action,
                category = action.category,
                targetType = targetType,
                targetId = targetId,
                description = description ?: action.description,
                requestPath = requestPath,
                requestMethod = requestMethod,
                requestBody = requestBody,
                responseStatus = responseStatus,
                ipAddress = ipAddress,
                userAgent = userAgent
            )
        }

        /**
         * 인증 이벤트용 (로그인 실패 등)
         */
        fun createAuthEvent(
            action: AuditAction,
            email: String?,
            ipAddress: String?,
            userAgent: String?,
            success: Boolean
        ): AuditLog {
            return AuditLog(
                userId = 0,
                userEmail = email,
                action = action,
                category = action.category,
                description = if (success) "성공" else "실패",
                ipAddress = ipAddress,
                userAgent = userAgent
            )
        }
    }
}
