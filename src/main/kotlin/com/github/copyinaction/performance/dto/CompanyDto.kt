package com.github.copyinaction.performance.dto

import com.github.copyinaction.performance.domain.Company
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "기획사/판매자 정보 응답 DTO")
data class CompanyResponse(
    @Schema(description = "회사 ID", example = "1")
    val id: Long,

    @Schema(description = "상호", example = "미쓰잭슨 주식회사")
    val name: String,

    @Schema(description = "대표자명", example = "박주영")
    val ceoName: String?,

    @Schema(description = "사업자등록번호", example = "564-88-01097")
    val businessNumber: String,

    @Schema(description = "이메일", example = "hello@msjackson.biz")
    val email: String?,

    @Schema(description = "연락처", example = "070-8834-5786")
    val contact: String?,

    @Schema(description = "주소", example = "서울특별시 중구 퇴계로 212(필동2가)")
    val address: String?,

    @Schema(description = "공연 문의 연락처/이메일", example = "mckithanhotel@msjackson.biz")
    val performanceInquiry: String?
) {
    companion object {
        fun from(company: Company): CompanyResponse {
            return CompanyResponse(
                id = company.id,
                name = company.name,
                ceoName = company.ceoName,
                businessNumber = company.businessNumber,
                email = company.email,
                contact = company.contact,
                address = company.address,
                performanceInquiry = company.performanceInquiry
            )
        }
    }
}

@Schema(description = "기획사/판매자 생성/수정 요청 DTO")
data class CompanyRequest(
    @field:NotBlank
    @Schema(description = "상호", example = "미쓰잭슨 주식회사", required = true)
    val name: String,

    @Schema(description = "대표자명", example = "박주영")
    val ceoName: String?,

    @field:NotBlank
    @Schema(description = "사업자등록번호", example = "564-88-01097", required = true)
    val businessNumber: String,

    @Schema(description = "이메일", example = "hello@msjackson.biz")
    val email: String?,

    @Schema(description = "연락처", example = "070-8834-5786")
    val contact: String?,

    @Schema(description = "주소", example = "서울특별시 중구 퇴계로 212(필동2가)")
    val address: String?,

    @Schema(description = "공연 문의 연락처/이메일", example = "mckithanhotel@msjackson.biz")
    val performanceInquiry: String?
)
