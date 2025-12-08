package com.github.copyinaction.repository

import com.github.copyinaction.domain.TicketOption
import org.springframework.data.jpa.repository.JpaRepository

interface TicketOptionRepository : JpaRepository<TicketOption, Long>
