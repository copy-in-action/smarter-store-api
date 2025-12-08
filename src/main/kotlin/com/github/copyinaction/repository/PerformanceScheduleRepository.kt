package com.github.copyinaction.repository

import com.github.copyinaction.domain.PerformanceSchedule
import org.springframework.data.jpa.repository.JpaRepository

interface PerformanceScheduleRepository : JpaRepository<PerformanceSchedule, Long>
