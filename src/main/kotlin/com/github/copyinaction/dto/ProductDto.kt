package com.github.copyinaction.dto

import com.github.copyinaction.domain.Product
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

@Schema(description = "상품 정보 응답 DTO")
data class ProductResponse(
    @Schema(description = "상품 ID", example = "1")
    val id: Long,

    @Schema(description = "상품명", example = "스마트폰")
    val name: String,

    @Schema(description = "상품 가격", example = "1000000.0")
    val price: Double,
) {
    companion object {
        fun from(product: Product): ProductResponse {
            return ProductResponse(
                id = product.id,
                name = product.name,
                price = product.price
            )
        }
    }
}

@Schema(description = "상품 생성 요청 DTO")
data class CreateProductRequest(
    @field:NotBlank(message = "상품명은 비워둘 수 없습니다.")
    @field:Size(max = 100, message = "상품명은 100자를 초과할 수 없습니다.")
    @Schema(description = "상품명", example = "새로운 스마트폰", required = true)
    val name: String,

    @field:Positive(message = "가격은 0보다 커야 합니다.")
    @Schema(description = "상품 가격", example = "1200000.0", required = true)
    val price: Double,
) {
    fun toEntity(): Product {
        return Product(
            name = this.name,
            price = this.price
        )
    }
}

@Schema(description = "상품 수정 요청 DTO")
data class UpdateProductRequest(
    @field:NotBlank(message = "상품명은 비워둘 수 없습니다.")
    @field:Size(max = 100, message = "상품명은 100자를 초과할 수 없습니다.")
    @Schema(description = "수정할 상품명", example = "업데이트된 스마트폰", required = true)
    val name: String,

    @field:Positive(message = "가격은 0보다 커야 합니다.")
    @Schema(description = "수정할 상품 가격", example = "1150000.0", required = true)
    val price: Double,
)
