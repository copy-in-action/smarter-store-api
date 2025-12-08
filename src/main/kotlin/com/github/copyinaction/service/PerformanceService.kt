package com.github.copyinaction.service

import com.github.copyinaction.domain.Performance
import com.github.copyinaction.dto.CreatePerformanceRequest
import com.github.copyinaction.dto.PerformanceResponse
import com.github.copyinaction.dto.UpdatePerformanceRequest
import com.github.copyinaction.exception.CustomException
import com.github.copyinaction.exception.ErrorCode
import com.github.copyinaction.repository.PerformanceRepository
import com.github.copyinaction.repository.VenueRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PerformanceService(
    private val performanceRepository: PerformanceRepository,
    private val venueRepository: VenueRepository
) {

    @Transactional
    fun createPerformance(request: CreatePerformanceRequest): PerformanceResponse {
        val venue = request.venueId?.let { 
            venueRepository.findById(it).orElseThrow { CustomException(ErrorCode.VENUE_NOT_FOUND) }
        }
        val performance = request.toEntity(venue)
        val savedPerformance = performanceRepository.save(performance)
        return PerformanceResponse.from(savedPerformance)
    }

    fun getPerformance(id: Long): PerformanceResponse {
        val performance = findPerformanceById(id)
        return PerformanceResponse.from(performance)
    }

    fun getAllPerformances(): List<PerformanceResponse> {
        return performanceRepository.findAll().map { PerformanceResponse.from(it) }
    }

    @Transactional
    fun updatePerformance(id: Long, request: UpdatePerformanceRequest): PerformanceResponse {
        val performance = findPerformanceById(id)
        val venue = request.venueId?.let {
            venueRepository.findById(it).orElseThrow { CustomException(ErrorCode.VENUE_NOT_FOUND) }
        }
        performance.update(
            title = request.title,
            description = request.description,
            category = request.category,
            runningTime = request.runningTime,
            ageRating = request.ageRating,
            mainImageUrl = request.mainImageUrl,
            venue = venue,
            startDate = request.startDate,
            endDate = request.endDate
        )
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
