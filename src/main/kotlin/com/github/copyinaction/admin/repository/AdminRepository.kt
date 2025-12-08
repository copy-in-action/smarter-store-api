package com.github.copyinaction.admin.repository

import com.github.copyinaction.admin.domain.Admin
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface AdminRepository : JpaRepository<Admin, Long> {
    fun findByLoginId(loginId: String): Optional<Admin>
}