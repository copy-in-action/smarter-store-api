package com.github.copyinaction.performance.domain

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "ticket_option")
class TicketOption(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id")
    var performance: Performance,

    var name: String,

    var price: BigDecimal,

    var totalQuantity: Int?

) : com.github.copyinaction.domain.BaseEntity()
