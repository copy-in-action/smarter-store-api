package com.github.copyinaction.performance.service

import com.github.copyinaction.common.service.FrontendRevalidationService
import com.github.copyinaction.performance.dto.CreatePerformanceRequest
import com.github.copyinaction.performance.dto.PerformanceResponse
import com.github.copyinaction.performance.dto.PerformanceSitemapResponse
import com.github.copyinaction.performance.dto.UpdatePerformanceRequest
import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.github.copyinaction.performance.domain.Performance
import com.github.copyinaction.performance.repository.CompanyRepository
import com.github.copyinaction.performance.repository.PerformanceRepository
import com.github.copyinaction.venue.repository.VenueRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PerformanceService(
    private val performanceRepository: PerformanceRepository,
    private val venueRepository: VenueRepository,
    private val companyRepository: CompanyRepository,
    private val frontendRevalidationService: FrontendRevalidationService
) {

    @Transactional
    fun createPerformance(request: CreatePerformanceRequest): PerformanceResponse {
        val venue = request.venueId?.let {
            venueRepository.findById(it).orElseThrow { CustomException(ErrorCode.VENUE_NOT_FOUND) }
        }
        val company = request.companyId?.let {
            companyRepository.findById(it).orElseThrow { CustomException(ErrorCode.COMPANY_NOT_FOUND) }
        }
        val performance = Performance.create(
            title = request.title,
            description = request.description,
            category = request.category,
            runningTime = request.runningTime,
            ageRating = request.ageRating,
            mainImageUrl = request.mainImageUrl,
            visible = request.visible,
            venue = venue,
            startDate = request.startDate,
            endDate = request.endDate,
            actors = request.actors,
            agency = request.agency,
            producer = request.producer,
            host = request.host,
            discountInfo = request.discountInfo,
            usageGuide = request.usageGuide,
            refundPolicy = request.refundPolicy,
            detailImageUrl = request.detailImageUrl,
            company = company,
            bookingFee = request.bookingFee,
            shippingGuide = request.shippingGuide
        )
        val savedPerformance = performanceRepository.save(performance)
        frontendRevalidationService.revalidateCache("performance-sitemap")
        return PerformanceResponse.from(savedPerformance)
    }

    fun getPerformance(id: Long): PerformanceResponse {
        val performance = findPerformanceById(id)
        return PerformanceResponse.Companion.from(performance)
    }

    fun getAllPerformances(): List<PerformanceResponse> {
        return performanceRepository.findAll().map { PerformanceResponse.Companion.from(it) }
    }

    fun getAllSitemapData(): List<PerformanceSitemapResponse> {
        return performanceRepository.findAllSitemapData()
    }

    @Transactional
    fun updatePerformance(id: Long, request: UpdatePerformanceRequest): PerformanceResponse {
        val performance = findPerformanceById(id)
        val venue = request.venueId?.let {
            venueRepository.findById(it).orElseThrow { CustomException(ErrorCode.VENUE_NOT_FOUND) }
        }
        val company = request.companyId?.let {
            companyRepository.findById(it).orElseThrow { CustomException(ErrorCode.COMPANY_NOT_FOUND) }
        }
        performance.update(
            title = request.title,
            description = request.description,
            category = request.category,
            runningTime = request.runningTime,
            ageRating = request.ageRating,
            mainImageUrl = request.mainImageUrl,
            visible = request.visible,
            venue = venue,
            startDate = request.startDate,
            endDate = request.endDate,
            actors = request.actors,
            agency = request.agency,
            producer = request.producer,
            host = request.host,
            discountInfo = request.discountInfo,
            usageGuide = request.usageGuide,
            refundPolicy = request.refundPolicy,
            detailImageUrl = request.detailImageUrl,
            company = company,
            bookingFee = request.bookingFee,
            shippingGuide = request.shippingGuide
        )
        frontendRevalidationService.revalidateCache("performance-sitemap")
        return PerformanceResponse.from(performance)
    }

    @Transactional
    fun deletePerformance(id: Long) {
        val performance = findPerformanceById(id)
        performanceRepository.delete(performance)
    }

    private fun findPerformanceById(id: Long): Performance {
        return performanceRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.PERFORMANCE_NOT_FOUND) }
    }
}