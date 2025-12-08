package com.github.copyinaction.performance.repository

import com.github.copyinaction.performance.domain.PerformanceSchedule
import org.springframework.data.jpa.repository.JpaRepository

interface PerformanceScheduleRepository : JpaRepository<PerformanceSchedule, Long>
