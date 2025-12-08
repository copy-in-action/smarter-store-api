package com.github.copyinaction.performance.repository

import com.github.copyinaction.performance.domain.Performance
import org.springframework.data.jpa.repository.JpaRepository

interface PerformanceRepository : JpaRepository<Performance, Long>
