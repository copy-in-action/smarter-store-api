# 통계/모니터링 API 설계

## 개정이력

| 버전 | 일자 | 작성자 | 내용 |
|------|------|--------|------|
| 1.0 | 2025-12-29 | Claude | 최초 작성 |

---

## 개요

공연별/스케줄별 매출 및 예매율 조회를 위한 API 설계 문서

### 목적
- 관리자 대시보드/메뉴에서 모니터링 용도
- 예매율은 사용자 프론트에도 노출 가능

### 설계 원칙
- **실시간 API**: 쿼리 기반, 현재 상태 조회
- **통계 API**: 배치 집계, 과거 데이터 분석
- **사용자용 API**: 캐시 활용, 간단한 정보만 노출

---

## 1. API 목록

### 1.1 실시간 API (관리자용)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/admin/schedules/{scheduleId}/seat-stats` | 스케줄별 좌석 현황 |
| GET | `/api/admin/performances/{performanceId}/stats` | 공연별 전체 현황 |
| GET | `/api/admin/performances/{performanceId}/schedules/stats` | 공연의 회차별 현황 목록 |
| GET | `/api/admin/dashboard/overview` | 대시보드 요약 (오늘 매출, 예매 등) |

### 1.2 통계 API (관리자용, 배치 기반)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/admin/stats/revenue` | 기간별 매출 통계 |
| GET | `/api/admin/stats/bookings` | 기간별 예매 통계 |
| GET | `/api/admin/stats/performances/ranking` | 공연별 매출 순위 |
| GET | `/api/admin/stats/daily/{date}` | 특정 일자 상세 통계 |

### 1.3 사용자용 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/schedules/{scheduleId}/availability` | 예매율/잔여석 (프론트 노출) |

---

## 2. API 상세 명세

### 2.1 스케줄별 좌석 현황

```
GET /api/admin/schedules/{scheduleId}/seat-stats
```

**Response**
```json
{
  "scheduleId": 1,
  "performanceId": 10,
  "performanceTitle": "레미제라블",
  "showDateTime": "2025-01-15T19:00:00",
  "saleStartDateTime": "2025-01-01T10:00:00",
  "venue": {
    "id": 1,
    "name": "예술의전당 오페라극장"
  },
  "seatSummary": {
    "totalSeats": 500,
    "reserved": 320,
    "pending": 15,
    "available": 165,
    "disabled": 0
  },
  "bookingRate": 64.0,
  "revenue": {
    "confirmed": 32000000,
    "pending": 1500000
  },
  "byGrade": [
    {
      "grade": "VIP",
      "totalSeats": 100,
      "reserved": 95,
      "pending": 3,
      "available": 2,
      "price": 150000,
      "revenue": 14250000
    },
    {
      "grade": "R",
      "totalSeats": 200,
      "reserved": 150,
      "pending": 10,
      "available": 40,
      "price": 120000,
      "revenue": 18000000
    },
    {
      "grade": "S",
      "totalSeats": 200,
      "reserved": 75,
      "pending": 2,
      "available": 123,
      "price": 80000,
      "revenue": 6000000
    }
  ]
}
```

### 2.2 공연별 전체 현황

```
GET /api/admin/performances/{performanceId}/stats
```

**Response**
```json
{
  "performanceId": 10,
  "title": "레미제라블",
  "period": {
    "startDate": "2025-01-10",
    "endDate": "2025-02-28"
  },
  "venue": {
    "id": 1,
    "name": "예술의전당 오페라극장"
  },
  "summary": {
    "totalSchedules": 50,
    "completedSchedules": 10,
    "upcomingSchedules": 40,
    "totalSeats": 25000,
    "totalReserved": 18125,
    "averageBookingRate": 72.5
  },
  "revenue": {
    "total": 125000000,
    "confirmed": 120000000,
    "pending": 5000000
  },
  "bookings": {
    "total": 1250,
    "confirmed": 1200,
    "cancelled": 50
  }
}
```

### 2.3 공연의 회차별 현황 목록

```
GET /api/admin/performances/{performanceId}/schedules/stats
    ?status=upcoming|completed|all (default: all)
    &sort=showDateTime|bookingRate|revenue (default: showDateTime)
    &order=asc|desc (default: asc)
```

**Response**
```json
{
  "performanceId": 10,
  "performanceTitle": "레미제라블",
  "schedules": [
    {
      "scheduleId": 1,
      "showDateTime": "2025-01-15T19:00:00",
      "status": "UPCOMING",
      "totalSeats": 500,
      "reserved": 320,
      "available": 165,
      "bookingRate": 64.0,
      "revenue": 32000000
    },
    {
      "scheduleId": 2,
      "showDateTime": "2025-01-16T14:00:00",
      "status": "UPCOMING",
      "totalSeats": 500,
      "reserved": 425,
      "available": 60,
      "bookingRate": 85.0,
      "revenue": 42500000
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 50,
    "totalPages": 3
  }
}
```

### 2.4 대시보드 요약

```
GET /api/admin/dashboard/overview
    ?date=2025-01-15 (default: today)
```

**Response**
```json
{
  "date": "2025-01-15",
  "today": {
    "revenue": 15000000,
    "bookings": 150,
    "cancellations": 5,
    "newUsers": 30
  },
  "comparison": {
    "revenueDiff": 2000000,
    "revenueDiffPercent": 15.4,
    "bookingsDiff": 20,
    "bookingsDiffPercent": 15.4
  },
  "upcomingSchedules": [
    {
      "scheduleId": 1,
      "performanceTitle": "레미제라블",
      "showDateTime": "2025-01-15T19:00:00",
      "bookingRate": 85.0
    }
  ],
  "topPerformances": [
    {
      "performanceId": 10,
      "title": "레미제라블",
      "todayRevenue": 5000000,
      "todayBookings": 50
    }
  ]
}
```

### 2.5 기간별 매출 통계

```
GET /api/admin/stats/revenue
    ?from=2025-01-01
    &to=2025-01-31
    &groupBy=daily|weekly|monthly (default: daily)
    &performanceId=10 (optional, 특정 공연만)
```

**Response**
```json
{
  "period": {
    "from": "2025-01-01",
    "to": "2025-01-31"
  },
  "groupBy": "daily",
  "summary": {
    "totalRevenue": 500000000,
    "totalBookings": 5000,
    "averageDailyRevenue": 16129032,
    "peakDate": "2025-01-15",
    "peakRevenue": 25000000
  },
  "data": [
    {
      "date": "2025-01-01",
      "revenue": 15000000,
      "bookings": 150,
      "cancellations": 5,
      "refunds": 500000
    },
    {
      "date": "2025-01-02",
      "revenue": 18000000,
      "bookings": 180,
      "cancellations": 3,
      "refunds": 300000
    }
  ]
}
```

### 2.6 공연별 매출 순위

```
GET /api/admin/stats/performances/ranking
    ?period=daily|weekly|monthly|yearly (default: monthly)
    &date=2025-01 (period에 따라 형식 다름)
    &limit=10 (default: 10)
```

**Response**
```json
{
  "period": "monthly",
  "date": "2025-01",
  "rankings": [
    {
      "rank": 1,
      "performanceId": 10,
      "title": "레미제라블",
      "venue": "예술의전당 오페라극장",
      "revenue": 125000000,
      "bookings": 1250,
      "averageBookingRate": 85.0,
      "change": 0
    },
    {
      "rank": 2,
      "performanceId": 15,
      "title": "오페라의 유령",
      "venue": "블루스퀘어",
      "revenue": 98000000,
      "bookings": 980,
      "averageBookingRate": 78.0,
      "change": 2
    }
  ]
}
```

### 2.7 사용자용 예매율/잔여석

```
GET /api/schedules/{scheduleId}/availability
```

**Response**
```json
{
  "scheduleId": 1,
  "bookingRate": 64,
  "availableSeats": 165,
  "status": "AVAILABLE",
  "byGrade": [
    { "grade": "VIP", "available": 2, "status": "ALMOST_FULL" },
    { "grade": "R", "available": 40, "status": "AVAILABLE" },
    { "grade": "S", "available": 123, "status": "AVAILABLE" }
  ]
}
```

**status 값**
- `AVAILABLE`: 잔여석 충분 (예매율 < 80%)
- `ALMOST_FULL`: 매진 임박 (예매율 >= 80%)
- `SOLD_OUT`: 매진 (예매율 = 100%)

---

## 3. 데이터 모델

### 3.1 일별 통계 테이블 (배치용)

```kotlin
@Entity
@Table(
    name = "daily_stats",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_daily_stats",
            columnNames = ["stat_date", "performance_id", "schedule_id"]
        )
    ],
    indexes = [
        Index(name = "idx_daily_stats_date", columnList = "stat_date"),
        Index(name = "idx_daily_stats_performance", columnList = "performance_id")
    ]
)
class DailyStats(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "stat_date", nullable = false)
    val statDate: LocalDate,

    @Column(name = "performance_id")
    val performanceId: Long?,      // null이면 전체 통계

    @Column(name = "schedule_id")
    val scheduleId: Long?,         // null이면 공연 전체 통계

    @Column(name = "total_bookings", nullable = false)
    val totalBookings: Int = 0,

    @Column(name = "total_cancellations", nullable = false)
    val totalCancellations: Int = 0,

    @Column(name = "total_revenue", nullable = false)
    val totalRevenue: Long = 0,

    @Column(name = "total_refunds", nullable = false)
    val totalRefunds: Long = 0,

    @Column(name = "reserved_seats", nullable = false)
    val reservedSeats: Int = 0,

    @Column(name = "booking_rate", precision = 5, scale = 2)
    val bookingRate: BigDecimal = BigDecimal.ZERO,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

### 3.2 등급별 일별 통계 테이블

```kotlin
@Entity
@Table(name = "daily_stats_by_grade")
class DailyStatsByGrade(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_stats_id")
    val dailyStats: DailyStats,

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_grade", nullable = false)
    val seatGrade: SeatGrade,

    @Column(name = "bookings", nullable = false)
    val bookings: Int = 0,

    @Column(name = "revenue", nullable = false)
    val revenue: Long = 0,

    @Column(name = "reserved_seats", nullable = false)
    val reservedSeats: Int = 0
)
```

---

## 4. 배치 설계

### 4.1 일별 통계 집계 배치

**실행 시간**: 매일 00:30 (전날 데이터 집계)

**처리 흐름**:
1. 전날 완료된 예매(CONFIRMED) 조회
2. 전날 취소된 예매(CANCELLED) 조회
3. 공연별/스케줄별 집계
4. DailyStats 테이블 저장

```kotlin
@Component
class DailyStatsJob(
    private val bookingRepository: BookingRepository,
    private val dailyStatsRepository: DailyStatsRepository
) {
    @Scheduled(cron = "0 30 0 * * *")
    @Transactional
    fun aggregateDailyStats() {
        val yesterday = LocalDate.now().minusDays(1)
        // 집계 로직
    }
}
```

### 4.2 실시간 캐시 갱신 (선택)

**용도**: 사용자용 예매율 API 캐싱

**방식**: Redis TTL 1분

```kotlin
@Cacheable(value = ["scheduleAvailability"], key = "#scheduleId")
fun getAvailability(scheduleId: Long): ScheduleAvailabilityResponse
```

---

## 5. 구현 순서

### Phase 1: 실시간 API (쿼리 기반)
1. `GET /api/admin/schedules/{scheduleId}/seat-stats`
2. `GET /api/admin/performances/{performanceId}/stats`
3. `GET /api/schedules/{scheduleId}/availability`

### Phase 2: 배치 및 통계 테이블
1. DailyStats 엔티티 생성
2. 배치 Job 구현
3. `GET /api/admin/stats/revenue`
4. `GET /api/admin/stats/performances/ranking`

### Phase 3: 대시보드 API
1. `GET /api/admin/dashboard/overview`
2. 캐시 적용 (Redis)

---

## 6. 성능 고려사항

### 6.1 실시간 API
- 좌석 수 계산: seatingChart JSON에서 총 좌석 수 캐싱
- 예약/점유 수: ScheduleSeatStatus 테이블 COUNT 쿼리
- 인덱스: `idx_schedule_seat_status_schedule` 활용

### 6.2 통계 API
- 배치 집계 데이터 사용으로 실시간 쿼리 부하 방지
- 기간 조회 시 파티셔닝 고려 (월별)

### 6.3 사용자용 API
- Redis 캐시 적용 (TTL 1분)
- 좌석 상태 변경 시 캐시 무효화 이벤트 발행

---

## 7. 권한

| API | 권한 |
|-----|------|
| `/api/admin/**` | ROLE_ADMIN |
| `/api/schedules/{id}/availability` | 인증 불필요 (공개) |
