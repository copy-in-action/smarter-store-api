package com.github.copyinaction.performance.controller

import com.github.copyinaction.common.exception.ErrorResponse
import com.github.copyinaction.performance.dto.CompanyRequest
import com.github.copyinaction.performance.dto.CompanyResponse
import com.github.copyinaction.performance.service.CompanyService
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
import org.springframework.web.bind.annotation.*
import java.net.URI

@Tag(name = "companies", description = "기획사/판매자 API - 기획사/판매자 정보 CRUD를 처리하는 API")
@RestController
@RequestMapping("/api/admin/companies")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
class CompanyController(
    private val companyService: CompanyService
) {

    @Operation(summary = "기획사 생성", description = "새로운 기획사/판매자 정보를 생성합니다.\n\n**권한: ADMIN**")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "기획사 생성 성공"),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 입력 값",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409",
            description = "이미 등록된 사업자등록번호",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping
    fun createCompany(@Valid @RequestBody request: CompanyRequest): ResponseEntity<CompanyResponse> {
        val company = companyService.createCompany(request)
        val location = URI.create("/api/admin/companies/${company.id}")
        return ResponseEntity.created(location).body(company)
    }

    @Operation(summary = "단일 기획사 조회", description = "ID로 특정 기획사/판매자 정보를 조회합니다.\n\n**권한: ADMIN**")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "기획사 조회 성공"),
        ApiResponse(
            responseCode = "404",
            description = "기획사를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping("/{id}")
    fun getCompany(
        @Parameter(description = "조회할 기획사 ID", required = true, example = "1")
        @PathVariable id: Long
    ): ResponseEntity<CompanyResponse> {
        val company = companyService.getCompany(id)
        return ResponseEntity.ok(company)
    }

    @Operation(summary = "모든 기획사 조회", description = "모든 기획사/판매자 목록을 조회합니다.\n\n**권한: ADMIN**")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "기획사 목록 조회 성공")
    )
    @GetMapping
    fun getAllCompanies(): ResponseEntity<List<CompanyResponse>> {
        val companies = companyService.getAllCompanies()
        return ResponseEntity.ok(companies)
    }

    @Operation(summary = "기획사 정보 수정", description = "특정 기획사/판매자 정보를 수정합니다.\n\n**권한: ADMIN**")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "기획사 정보 수정 성공"),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 입력 값",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "기획사를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409",
            description = "이미 등록된 사업자등록번호",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PutMapping("/{id}")
    fun updateCompany(
        @Parameter(description = "수정할 기획사 ID", required = true, example = "1")
        @PathVariable id: Long,
        @Valid @RequestBody request: CompanyRequest
    ): ResponseEntity<CompanyResponse> {
        val company = companyService.updateCompany(id, request)
        return ResponseEntity.ok(company)
    }

    @Operation(summary = "기획사 삭제", description = "특정 기획사/판매자를 삭제합니다.\n\n**권한: ADMIN**")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "기획사 삭제 성공"),
        ApiResponse(
            responseCode = "404",
            description = "기획사를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @DeleteMapping("/{id}")
    fun deleteCompany(
        @Parameter(description = "삭제할 기획사 ID", required = true, example = "1")
        @PathVariable id: Long
    ): ResponseEntity<Unit> {
        companyService.deleteCompany(id)
        return ResponseEntity.noContent().build()
    }
}
