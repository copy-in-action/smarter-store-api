package com.github.copyinaction.repository

import com.github.copyinaction.domain.Performance
import org.springframework.data.jpa.repository.JpaRepository

interface PerformanceRepository : JpaRepository<Performance, Long>
