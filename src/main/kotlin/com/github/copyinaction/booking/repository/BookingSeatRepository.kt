package com.github.copyinaction.booking.repository

import com.github.copyinaction.booking.domain.BookingSeat
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BookingSeatRepository : JpaRepository<BookingSeat, Long>
