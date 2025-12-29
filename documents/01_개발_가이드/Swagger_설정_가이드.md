# Swagger (OpenAPI) 가이드: API 문서 자동화

이 문서는 Spring Boot 프로젝트에서 `springdoc-openapi` 라이브러리를 사용하여 API 문서를 자동으로 생성하고 관리하는 방법에 대해 상세히 설명합니다.

## 1. Swagger (OpenAPI)란?

Swagger는 RESTful API를 설계, 빌드, 문서화, 사용하는 데 도움이 되는 도구 세트입니다. OpenAPI Specification (OAS)은 RESTful API를 기계가 읽고 사람이 이해할 수 있는 방식으로 정의하는 표준을 제공하며, Swagger는 이 OAS를 구현한 도구 중 하나입니다.

`springdoc-openapi`는 Spring Boot 애플리케이션에서 OpenAPI 3 사양을 기반으로 자동으로 API 문서를 생성해주는 라이브러리입니다.

## 2. 왜 API 문서를 자동화해야 하는가?

*   **협업 용이성**: 프론트엔드 개발자, 모바일 개발자, 또는 다른 백엔드 팀과 API 명세를 공유하고 소통하는 데 필수적입니다.
*   **생산성 향상**: 수동으로 문서를 작성하고 업데이트하는 시간과 노력을 절약합니다. 코드 변경 시 문서도 자동으로 업데이트됩니다.
*   **정확성**: 코드에 기반하여 문서를 생성하므로, 실제 API와 문서 간의 불일치(Drift)를 최소화할 수 있습니다.
*   **테스트 용이성**: Swagger UI를 통해 브라우저에서 직접 API를 호출하고 테스트해 볼 수 있습니다.

## 3. 적용 방법

### 3.1. 의존성 추가

`build.gradle.kts` 파일에 `springdoc-openapi-starter-webmvc-ui` 의존성을 추가하여 기능을 활성화합니다. (이 작업은 이미 완료되었습니다.)

```kotlin
// build.gradle.kts 예시 (API Documentation 섹션)
dependencies {
    // ...
    // API Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
    // ...
}
```

### 3.2. 사용 방법 (주요 어노테이션)

SpringDoc은 OpenAPI 3 사양을 따르며, 코드 내에 특정 어노테이션을 추가하여 API 문서를 상세하게 정의할 수 있습니다.

| 어노테이션 | 설명 | 적용 위치 |
| :---------------- | :---------------------------------------------------------------------------------------------------------------------------------------------------------- | :---------- |
| `@Tag` | 컨트롤러 단위로 API를 그룹화하고, API 그룹에 대한 설명을 제공합니다. | 컨트롤러 클래스 |
| `@Operation` | 각 API(메서드)의 기능 요약 및 설명을 추가합니다. Swagger UI에서 API를 선택했을 때 보여지는 핵심 정보입니다. | 컨트롤러 메서드 |
| `@ApiResponses` | API가 반환할 수 있는 모든 응답(성공/실패) 케이스를 정의하는 컨테이너 어노테이션입니다. | 컨트롤러 메서드 |
| `@ApiResponse` | 개별 응답의 HTTP 상태 코드, 설명, 그리고 해당 응답에서 반환되는 데이터의 스키마(`content = [Content(schema = Schema(implementation = YourDto::class))]`)를 정의합니다. | `@ApiResponses` 내부 |
| `@Parameter` | 경로 변수(Path Variable)나 쿼리 파라미터에 대한 설명, 필수 여부, 예시 값 등을 추가합니다. | 컨트롤러 메서드의 파라미터 |
| `@Schema` | DTO(데이터 모델) 클래스나 그 필드에 대한 설명, 예시 값, 제약 조건 등을 추가합니다. 이는 요청/응답 데이터의 구조를 문서화합니다. | DTO 클래스 및 필드 |

### 3.3. API 문서 접근

애플리케이션을 실행한 후, 웹 브라우저에서 다음 URL로 접속하면 Swagger UI를 통해 자동으로 생성된 API 문서를 확인하고 직접 API 호출을 테스트해볼 수 있습니다.

*   **Swagger UI**: `http://localhost:8080/swagger-ui.html`
*   **OpenAPI Specification (JSON)**: `http://localhost:8080/v3/api-docs`

## 4. 상세 예시

### 4.1. DTO에 `@Schema` 적용 (`dto/ProductDto.kt` 예시)

DTO 클래스와 그 필드에 `@Schema` 어노테이션을 추가하여 데이터 모델에 대한 설명을 제공합니다.

```kotlin
// ProductDto.kt 예시
package com.github.copyinaction.dto

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
) { /* ... */ }

@Schema(description = "상품 생성 요청 DTO")
data class CreateProductRequest(
    @field:NotBlank(message = "상품명은 비워둘 수 없습니다.")
    @field:Size(max = 100, message = "상품명은 100자를 초과할 수 없습니다.")
    @Schema(description = "상품명", example = "새로운 스마트폰", required = true)
    val name: String,

    @field:Positive(message = "가격은 0보다 커야 합니다.")
    @Schema(description = "상품 가격", example = "1200000.0", required = true)
    val price: Double,
) { /* ... */ }

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
```

### 4.2. Controller에 `@Tag`, `@Operation`, `@ApiResponses`, `@Parameter` 적용 (`controller/ProductController.kt` 예시)

컨트롤러 클래스에는 `@Tag`를, 각 메서드에는 `@Operation`으로 설명을, `@ApiResponses`와 `@ApiResponse`로 다양한 응답 케이스를, `@Parameter`로 경로 변수를 문서화합니다. 에러 응답은 `ErrorResponse` DTO의 스키마를 참조합니다.

```kotlin
// ProductController.kt 예시
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
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize // @PreAuthorize를 위한 import
import org.springframework.web.bind.annotation.*
import java.net.URI

@Tag(name = "상품 API", description = "상품 관련 CRUD를 처리하는 API")
@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService,
) {
    @Operation(summary = "상품 생성", description = "새로운 상품 정보를 생성합니다.")
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
    @PreAuthorize("hasRole('ADMIN')") // ADMIN 권한만 허용
    @PostMapping
    fun createProduct(@Valid @RequestBody request: CreateProductRequest): ResponseEntity<ProductResponse> {
        val product = productService.createProduct(request)
        val location = URI.create("/api/products/${product.id}")
        return ResponseEntity.created(location).body(product)
    }

    @Operation(summary = "단일 상품 조회", description = "ID로 특정 상품의 정보를 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "상품 조회 성공"),
        ApiResponse(
            responseCode = "404", description = "상품을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403", description = "접근 권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')") // USER, ADMIN 권한 허용
    @GetMapping("/{id}")
    fun getProduct(
        @Parameter(description = "조회할 상품의 ID", required = true, example = "1") @PathVariable id: Long
    ): ResponseEntity<ProductResponse> {
        val product = productService.getProduct(id)
        return ResponseEntity.ok(product)
    }

    @Operation(summary = "모든 상품 조회", description = "모든 상품 목록을 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "상품 목록 조회 성공"),
        ApiResponse(
            responseCode = "403", description = "접근 권한 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')") // USER, ADMIN 권한 허용
    @GetMapping
    fun getAllProducts(): ResponseEntity<List<ProductResponse>> {
        val products = productService.getAllProducts()
        return ResponseEntity.ok(products)
    }

    @Operation(summary = "상품 정보 수정", description = "특정 상품의 정보를 수정합니다.")
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
    @PreAuthorize("hasRole('ADMIN')") // ADMIN 권한만 허용
    @PutMapping("/{id}")
    fun updateProduct(
        @Parameter(description = "수정할 상품의 ID", required = true, example = "1") @PathVariable id: Long,
        @Valid @RequestBody request: UpdateProductRequest
    ): ResponseEntity<ProductResponse> {
        val product = productService.updateProduct(id, request)
        return ResponseEntity.ok(product)
    }

    @Operation(summary = "상품 삭제", description = "특정 상품을 삭제합니다.")
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
    @PreAuthorize("hasRole('ADMIN')") // ADMIN 권한만 허용
    @DeleteMapping("/{id}")
    fun deleteProduct(
        @Parameter(description = "삭제할 상품의 ID", required = true, example = "1") @PathVariable id: Long
    ): ResponseEntity<Unit> {
        productService.deleteProduct(id)
        return ResponseEntity.noContent().build()
    }
}
```
