package com.github.copyinaction.performance.repository

import com.github.copyinaction.booking.domain.BookingStatus
import com.github.copyinaction.booking.domain.QBooking.booking
import com.github.copyinaction.performance.domain.*
import com.github.copyinaction.performance.domain.QPerformance.performance
import com.github.copyinaction.performance.domain.QPerformanceSchedule.performanceSchedule
import com.github.copyinaction.performance.dto.*
import com.github.copyinaction.venue.domain.QVenue.venue
import com.github.copyinaction.wishlist.domain.QWishlist.wishlist
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.LocalDateTime

class PerformanceRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : PerformanceRepositoryCustom {

    override fun searchPerformances(
        request: PerformanceSearchRequest
    ): Page<PerformanceSearchResponse> {
        val pageable = PageRequest.of(request.page, request.size)
        val now = LocalDate.now()

        // 1. 메인 쿼리 (데이터 조회)
        val query = queryFactory
            .select(
                Projections.constructor(
                    PerformanceSearchResponse::class.java,
                    performance.id,
                    performance.title,
                    performance.mainImageUrl,
                    performance.category,
                    venue.address,
                    venue.address,
                    performance.startDate,
                    performance.endDate
                )
            )
            .from(performance)
            .leftJoin(performance.venue, venue)
            .where(
                performance.visible.isTrue,
                keywordContains(request.keyword),
                statusFilter(request.status, now),
                categoryFilter(request.category),
                regionFilter(request.region)
            )

        // 2. 정렬 적용
        applySorting(query, request.sort)

        // 3. 페이징 적용
        val content = query
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        // 4. 전체 개수 쿼리
        val total = queryFactory
            .select(performance.count())
            .from(performance)
            .leftJoin(performance.venue, venue)
            .where(
                performance.visible.isTrue,
                keywordContains(request.keyword),
                statusFilter(request.status, now),
                categoryFilter(request.category),
                regionFilter(request.region)
            )
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    override fun autocompletePerformances(keyword: String?): List<PerformanceAutocompleteResponse> {
        if (keyword.isNullOrBlank()) return emptyList()

        val now = LocalDate.now()
        
        return queryFactory
            .select(
                Projections.constructor(
                    PerformanceAutocompleteResponse::class.java,
                    performance.id,
                    performance.title,
                    performance.mainImageUrl,
                    performance.category,
                    venue.address // 주소로 반환 후 Service에서 Region 매핑
                )
            )
            .from(performance)
            .leftJoin(performance.venue, venue)
            .where(
                performance.visible.isTrue,
                keywordContains(keyword),
                // 자동완성은 판매 종료된 것은 제외 (예정/중인 것만)
                performance.endDate.goe(now)
            )
            .limit(6)
            .fetch()
    }

    // ===== 조건절 Helper 메서드 =====

    private fun keywordContains(keyword: String?): com.querydsl.core.types.Predicate? {
        if (keyword.isNullOrBlank()) return null
        return performance.title.containsIgnoreCase(keyword)
            .or(performance.category.containsIgnoreCase(keyword))
            .or(venue.address.containsIgnoreCase(keyword))
    }

    private fun categoryFilter(categories: List<String>?): com.querydsl.core.types.Predicate? {
        return if (categories.isNullOrEmpty()) null else performance.category.`in`(categories)
    }

    private fun regionFilter(regions: List<Region>?): com.querydsl.core.types.Predicate? {
        if (regions.isNullOrEmpty()) return null
        val builder = BooleanBuilder()
        regions.forEach { region ->
            region.keywords.forEach { keyword ->
                builder.or(venue.address.contains(keyword))
            }
        }
        return builder
    }

    private fun statusFilter(statuses: List<PerformanceSearchStatus>?, now: LocalDate): com.querydsl.core.types.Predicate? {
        if (statuses.isNullOrEmpty()) return null
        val builder = BooleanBuilder()
        statuses.forEach { status ->
            when (status) {
                PerformanceSearchStatus.UPCOMING -> builder.or(performance.startDate.gt(now))
                PerformanceSearchStatus.ON_SALE -> builder.or(performance.startDate.loe(now).and(performance.endDate.goe(now)))
                PerformanceSearchStatus.CLOSED -> builder.or(performance.endDate.lt(now))
            }
        }
        return builder
    }

    private fun applySorting(query: com.querydsl.jpa.impl.JPAQuery<*>, sort: PerformanceSearchSort?) {
        when (sort) {
            PerformanceSearchSort.BOOKING_COUNT -> {
                // 예매 많은 순: Booking 테이블 조인 및 카운트
                // Performance -> PerformanceSchedule -> Booking
                query.leftJoin(performanceSchedule).on(performanceSchedule.performance.id.eq(performance.id))
                    .leftJoin(booking).on(booking.schedule.id.eq(performanceSchedule.id).and(booking.bookingStatus.eq(BookingStatus.CONFIRMED)))
                    .groupBy(performance.id)
                    .orderBy(booking.count().desc(), performance.createdAt.desc())
            }
            PerformanceSearchSort.END_DATE_ASC -> {
                query.orderBy(performance.endDate.asc(), performance.createdAt.desc())
            }
            PerformanceSearchSort.CREATED_AT_DESC -> {
                query.orderBy(performance.createdAt.desc())
            }
            else -> {
                query.orderBy(performance.createdAt.desc())
            }
        }
    }
}
