package com.github.copyinaction.performance.service

import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.performance.domain.Company
import com.github.copyinaction.performance.dto.CompanyRequest
import com.github.copyinaction.performance.dto.CompanyResponse
import com.github.copyinaction.performance.repository.CompanyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CompanyService(
    private val companyRepository: CompanyRepository
) {
    @Transactional
    fun createCompany(request: CompanyRequest): CompanyResponse {
        if (companyRepository.existsByBusinessNumber(request.businessNumber)) {
            throw CustomException(ErrorCode.COMPANY_ALREADY_EXISTS)
        }
        val company = Company.create(
            name = request.name,
            ceoName = request.ceoName,
            businessNumber = request.businessNumber,
            email = request.email,
            contact = request.contact,
            address = request.address,
            performanceInquiry = request.performanceInquiry
        )
        val savedCompany = companyRepository.save(company)
        return CompanyResponse.from(savedCompany)
    }

    fun getCompany(id: Long): CompanyResponse {
        val company = findCompanyById(id)
        return CompanyResponse.from(company)
    }

    fun getAllCompanies(): List<CompanyResponse> {
        return companyRepository.findAll().map { CompanyResponse.from(it) }
    }

    @Transactional
    fun updateCompany(id: Long, request: CompanyRequest): CompanyResponse {
        val company = findCompanyById(id)

        if (company.businessNumber != request.businessNumber &&
            companyRepository.existsByBusinessNumber(request.businessNumber)) {
            throw CustomException(ErrorCode.COMPANY_ALREADY_EXISTS)
        }

        company.update(
            name = request.name,
            ceoName = request.ceoName,
            businessNumber = request.businessNumber,
            email = request.email,
            contact = request.contact,
            address = request.address,
            performanceInquiry = request.performanceInquiry
        )
        return CompanyResponse.from(company)
    }

    @Transactional
    fun deleteCompany(id: Long) {
        val company = findCompanyById(id)
        companyRepository.delete(company)
    }


    private fun findCompanyById(id: Long): Company {
        return companyRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.COMPANY_NOT_FOUND) }
    }
}
