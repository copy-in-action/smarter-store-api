package com.github.copyinaction.performance.repository

import com.github.copyinaction.performance.domain.TicketOption
import org.springframework.data.jpa.repository.JpaRepository

interface TicketOptionRepository : JpaRepository<TicketOption, Long>
