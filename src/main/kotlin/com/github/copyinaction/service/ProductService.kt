package com.github.copyinaction.service

import com.github.copyinaction.domain.Product
import com.github.copyinaction.dto.CreateProductRequest
import com.github.copyinaction.dto.ProductResponse
import com.github.copyinaction.dto.UpdateProductRequest
import com.github.copyinaction.exception.CustomException
import com.github.copyinaction.exception.ErrorCode
import com.github.copyinaction.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProductService(
    private val productRepository: ProductRepository,
) {

    /**
     * 상품 생성
     */
    @Transactional
    fun createProduct(request: CreateProductRequest): ProductResponse {
        val product = request.toEntity()
        val savedProduct = productRepository.save(product)
        return ProductResponse.from(savedProduct)
    }

    /**
     * ID로 단일 상품 조회
     */
    fun getProduct(id: Long): ProductResponse {
        val product = findProductById(id)
        return ProductResponse.from(product)
    }

    /**
     * 모든 상품 조회
     */
    fun getAllProducts(): List<ProductResponse> {
        return productRepository.findAll().map { ProductResponse.from(it) }
    }

    /**
     * 상품 정보 수정
     */
    @Transactional
    fun updateProduct(id: Long, request: UpdateProductRequest): ProductResponse {
        val product = findProductById(id)
        product.update(name = request.name, price = request.price)
        return ProductResponse.from(product)
    }

    /**
     * 상품 삭제
     */
    @Transactional
    fun deleteProduct(id: Long) {
        val product = findProductById(id)
        productRepository.delete(product)
    }

    private fun findProductById(id: Long): Product {
        return productRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.PRODUCT_NOT_FOUND) }
    }
}
