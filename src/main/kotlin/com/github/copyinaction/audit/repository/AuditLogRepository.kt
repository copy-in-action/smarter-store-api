package com.github.copyinaction.audit.repository

import com.github.copyinaction.audit.domain.AuditAction
import com.github.copyinaction.audit.domain.AuditCategory
import com.github.copyinaction.audit.domain.AuditLog
import com.github.copyinaction.audit.domain.AuditTargetType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface AuditLogRepository : JpaRepository<AuditLog, Long> {

    fun findByUserId(userId: Long, pageable: Pageable): Page<AuditLog>

    fun findByAction(action: AuditAction, pageable: Pageable): Page<AuditLog>

    fun findByCategory(category: AuditCategory, pageable: Pageable): Page<AuditLog>

    fun findByTargetTypeAndTargetId(
        targetType: AuditTargetType,
        targetId: String,
        pageable: Pageable
    ): Page<AuditLog>

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:userId IS NULL OR a.userId = :userId)
        AND (:action IS NULL OR a.action = :action)
        AND (:category IS NULL OR a.category = :category)
        AND (:targetType IS NULL OR a.targetType = :targetType)
        AND (:targetId IS NULL OR a.targetId = :targetId)
        AND (:from IS NULL OR a.createdAt >= :from)
        AND (:to IS NULL OR a.createdAt <= :to)
        ORDER BY a.createdAt DESC
    """)
    fun findByFilters(
        @Param("userId") userId: Long?,
        @Param("action") action: AuditAction?,
        @Param("category") category: AuditCategory?,
        @Param("targetType") targetType: AuditTargetType?,
        @Param("targetId") targetId: String?,
        @Param("from") from: LocalDateTime?,
        @Param("to") to: LocalDateTime?,
        pageable: Pageable
    ): Page<AuditLog>

    @Query("""
        SELECT a.action, COUNT(a)
        FROM AuditLog a
        WHERE a.createdAt >= :from AND a.createdAt <= :to
        GROUP BY a.action
        ORDER BY COUNT(a) DESC
    """)
    fun countByActionBetween(
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime
    ): List<Array<Any>>

    @Query("""
        SELECT a.category, COUNT(a)
        FROM AuditLog a
        WHERE a.createdAt >= :from AND a.createdAt <= :to
        GROUP BY a.category
    """)
    fun countByCategoryBetween(
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime
    ): List<Array<Any>>

    fun countByCreatedAtBetween(from: LocalDateTime, to: LocalDateTime): Long
}
