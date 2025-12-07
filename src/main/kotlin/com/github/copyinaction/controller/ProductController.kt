package com.github.copyinaction.controller

import com.github.copyinaction.dto.CreateProductRequest
import com.github.copyinaction.dto.ProductResponse
import com.github.copyinaction.dto.UpdateProductRequest
import com.github.copyinaction.exception.ErrorResponse
import com.github.copyinaction.service.ProductService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@Tag(name = "products", description = "상품 API - 상품 관련 CRUD를 처리하는 API")
@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService,
) {

    @Operation(summary = "상품 생성", description = "새로운 상품 정보를 생성합니다.\n\n**권한: ADMIN**")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "상품 생성 성공"),
        ApiResponse(
            responseCode = "400", description = "잘못된 입력 값",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403", description = "접근 권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    fun createProduct(@Valid @RequestBody request: CreateProductRequest): ResponseEntity<ProductResponse> {
        val product = productService.createProduct(request)
        // 생성된 리소스의 URI를 Location 헤더에 담아 201 Created 응답 반환
        val location = URI.create("/api/products/${product.id}")
        return ResponseEntity.created(location).body(product)
    }

    @Operation(summary = "단일 상품 조회", description = "ID로 특정 상품의 정보를 조회합니다.\n\n**권한: USER, ADMIN**")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "상품 조회 성공"),
        ApiResponse(
            responseCode = "404", description = "상품을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/{id}")
    fun getProduct(
        @Parameter(description = "조회할 상품의 ID", required = true, example = "1") @PathVariable id: Long
    ): ResponseEntity<ProductResponse> {
        val product = productService.getProduct(id)
        return ResponseEntity.ok(product)
    }

    @Operation(summary = "모든 상품 조회", description = "모든 상품 목록을 조회합니다.\n\n**권한: USER, ADMIN**")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "상품 목록 조회 성공"),
        ApiResponse(
            responseCode = "403", description = "접근 권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping
    fun getAllProducts(): ResponseEntity<List<ProductResponse>> {
        val products = productService.getAllProducts()
        return ResponseEntity.ok(products)
    }

    @Operation(summary = "상품 정보 수정", description = "특정 상품의 정보를 수정합니다.\n\n**권한: ADMIN**")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "상품 정보 수정 성공"),
        ApiResponse(
            responseCode = "400", description = "잘못된 입력 값",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404", description = "상품을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403", description = "접근 권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    fun updateProduct(
        @Parameter(description = "수정할 상품의 ID", required = true, example = "1") @PathVariable id: Long,
        @Valid @RequestBody request: UpdateProductRequest
    ): ResponseEntity<ProductResponse> {
        val product = productService.updateProduct(id, request)
        return ResponseEntity.ok(product)
    }

    @Operation(summary = "상품 삭제", description = "특정 상품을 삭제합니다.\n\n**권한: ADMIN**")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "상품 삭제 성공"),
        ApiResponse(
            responseCode = "404", description = "상품을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403", description = "접근 권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    fun deleteProduct(
        @Parameter(description = "삭제할 상품의 ID", required = true, example = "1") @PathVariable id: Long
    ): ResponseEntity<Unit> {
        productService.deleteProduct(id)
        return ResponseEntity.noContent().build()
    }
}