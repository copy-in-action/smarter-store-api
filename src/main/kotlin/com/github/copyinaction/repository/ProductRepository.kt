package com.github.copyinaction.repository

import com.github.copyinaction.domain.Product
import org.springframework.data.jpa.repository.JpaRepository

interface ProductRepository : JpaRepository<Product, Long>
