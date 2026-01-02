package com.github.copyinaction.common.domain

import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Transient
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.domain.AfterDomainEventPublication
import org.springframework.data.domain.DomainEvents
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    @CreatedDate
    var createdAt: LocalDateTime? = null

    @LastModifiedDate
    var updatedAt: LocalDateTime? = null

    @Transient
    private val domainEvents = mutableListOf<Any>()

    fun registerEvent(event: Any) {
        domainEvents.add(event)
    }

    @DomainEvents
    protected fun domainEvents(): Collection<Any> = domainEvents.toList()

    @AfterDomainEventPublication
    protected fun clearDomainEvents() = domainEvents.clear()
}
