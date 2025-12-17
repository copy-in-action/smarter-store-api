package com.github.copyinaction.performance.repository

import com.github.copyinaction.performance.domain.Company
import org.springframework.data.jpa.repository.JpaRepository

interface CompanyRepository : JpaRepository<Company, Long> {
    fun existsByBusinessNumber(businessNumber: String): Boolean
}
