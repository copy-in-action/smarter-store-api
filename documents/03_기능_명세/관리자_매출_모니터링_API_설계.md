# 관리자 매출 모니터링 API 설계

## 개정이력
| 버전 | 일자 | 작성자 | 내용 |
|------|------|--------|------|
| 1.0 | 2025-12-31 | Claude | 최초 작성 |

---

## 1. 개요

### 1.1 목적
관리자 대시보드에서 공연별 매출 현황, 예매 통계, 실시간 판매 추이 등을 모니터링할 수 있는 API 제공

### 1.2 주요 기능
- 전체 매출 요약 (대시보드 메인)
- 공연별 매출 현황
- 기간별 매출 통계
- 회차별 판매 현황
- 좌석 등급별 매출 분석

---

## 2. API 목록

| API | Method | 경로 | 설명 |
|-----|--------|------|------|
| 대시보드 요약 | GET | `/api/admin/dashboard/summary` | 전체 매출/예매 요약 |
| 공연별 매출 | GET | `/api/admin/dashboard/performances` | 공연별 매출 목록 |
| 공연 상세 매출 | GET | `/api/admin/dashboard/performances/{id}` | 특정 공연 상세 매출 |
| 기간별 매출 | GET | `/api/admin/dashboard/sales/daily` | 일별 매출 추이 |
| 회차별 판매 | GET | `/api/admin/dashboard/schedules/{id}/sales` | 특정 회차 판매 현황 |
| 실시간 예매 | GET | `/api/admin/dashboard/bookings/recent` | 최근 예매 내역 |

---

## 3. API 상세 설계

### 3.1 대시보드 요약 API

**Endpoint:** `GET /api/admin/dashboard/summary`

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| startDate | LocalDate | N | 조회 시작일 (기본: 오늘) |
| endDate | LocalDate | N | 조회 종료일 (기본: 오늘) |

**Response:**
```json
{
  "period": {
    "startDate": "2025-12-01",
    "endDate": "2025-12-31"
  },
  "totalRevenue": 15000000,
  "totalBookings": 320,
  "confirmedBookings": 280,
  "cancelledBookings": 25,
  "expiredBookings": 15,
  "averageTicketPrice": 46875,
  "totalTicketsSold": 520,
  "comparisonWithPrevious": {
    "revenueChangePercent": 12.5,
    "bookingsChangePercent": 8.3
  }
}
```

---

### 3.2 공연별 매출 목록 API

**Endpoint:** `GET /api/admin/dashboard/performances`

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| startDate | LocalDate | N | 조회 시작일 |
| endDate | LocalDate | N | 조회 종료일 |
| sortBy | String | N | 정렬 기준 (revenue, bookings, ticketsSold) |
| sortDirection | String | N | 정렬 방향 (asc, desc) |
| page | Int | N | 페이지 번호 (기본: 0) |
| size | Int | N | 페이지 크기 (기본: 20) |

**Response:**
```json
{
  "content": [
    {
      "performanceId": 1,
      "title": "뮤지컬 위키드",
      "category": "뮤지컬",
      "venue": "블루스퀘어 신한카드홀",
      "period": {
        "startDate": "2025-12-01",
        "endDate": "2026-02-28"
      },
      "totalRevenue": 8500000,
      "totalBookings": 180,
      "confirmedBookings": 165,
      "cancelledBookings": 10,
      "totalTicketsSold": 320,
      "averageTicketPrice": 26562,
      "salesRate": 75.5
    }
  ],
  "totalElements": 15,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

---

### 3.3 공연 상세 매출 API

**Endpoint:** `GET /api/admin/dashboard/performances/{performanceId}`

**Response:**
```json
{
  "performance": {
    "id": 1,
    "title": "뮤지컬 위키드",
    "category": "뮤지컬",
    "venue": "블루스퀘어 신한카드홀",
    "period": {
      "startDate": "2025-12-01",
      "endDate": "2026-02-28"
    }
  },
  "summary": {
    "totalRevenue": 8500000,
    "totalBookings": 180,
    "confirmedBookings": 165,
    "cancelledBookings": 10,
    "expiredBookings": 5,
    "totalTicketsSold": 320,
    "totalSeats": 500,
    "salesRate": 64.0
  },
  "revenueByGrade": [
    {
      "grade": "VIP",
      "revenue": 4500000,
      "ticketsSold": 45,
      "averagePrice": 100000
    },
    {
      "grade": "R",
      "revenue": 2800000,
      "ticketsSold": 70,
      "averagePrice": 40000
    },
    {
      "grade": "S",
      "revenue": 1200000,
      "ticketsSold": 60,
      "averagePrice": 20000
    }
  ],
  "schedules": [
    {
      "scheduleId": 101,
      "performanceDate": "2025-12-25T14:00:00",
      "totalSeats": 100,
      "soldSeats": 85,
      "revenue": 2500000,
      "salesRate": 85.0
    }
  ],
  "dailySales": [
    {
      "date": "2025-12-20",
      "revenue": 500000,
      "bookings": 12
    }
  ]
}
```

---

### 3.4 일별 매출 추이 API

**Endpoint:** `GET /api/admin/dashboard/sales/daily`

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| startDate | LocalDate | Y | 조회 시작일 |
| endDate | LocalDate | Y | 조회 종료일 |
| performanceId | Long | N | 특정 공연 필터 |

**Response:**
```json
{
  "period": {
    "startDate": "2025-12-01",
    "endDate": "2025-12-31"
  },
  "totalRevenue": 15000000,
  "totalBookings": 320,
  "dailyData": [
    {
      "date": "2025-12-01",
      "revenue": 450000,
      "bookings": 12,
      "ticketsSold": 18
    },
    {
      "date": "2025-12-02",
      "revenue": 680000,
      "bookings": 15,
      "ticketsSold": 22
    }
  ]
}
```

---

### 3.5 회차별 판매 현황 API

**Endpoint:** `GET /api/admin/dashboard/schedules/{scheduleId}/sales`

**Response:**
```json
{
  "schedule": {
    "id": 101,
    "performanceId": 1,
    "performanceTitle": "뮤지컬 위키드",
    "performanceDate": "2025-12-25T14:00:00",
    "venue": "블루스퀘어 신한카드홀"
  },
  "sales": {
    "totalRevenue": 2500000,
    "totalBookings": 45,
    "confirmedBookings": 42,
    "cancelledBookings": 3,
    "totalSeats": 100,
    "soldSeats": 85,
    "pendingSeats": 5,
    "availableSeats": 10,
    "salesRate": 85.0
  },
  "salesByGrade": [
    {
      "grade": "VIP",
      "totalSeats": 20,
      "soldSeats": 18,
      "pendingSeats": 1,
      "revenue": 1800000,
      "salesRate": 90.0
    },
    {
      "grade": "R",
      "totalSeats": 40,
      "soldSeats": 35,
      "pendingSeats": 2,
      "revenue": 700000,
      "salesRate": 87.5
    }
  ],
  "recentBookings": [
    {
      "bookingId": "550e8400-e29b-41d4-a716-446655440000",
      "bookingNumber": "BK20251225001",
      "status": "CONFIRMED",
      "totalPrice": 150000,
      "ticketCount": 2,
      "createdAt": "2025-12-20T15:30:00"
    }
  ]
}
```

---

### 3.6 최근 예매 내역 API

**Endpoint:** `GET /api/admin/dashboard/bookings/recent`

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| limit | Int | N | 조회 건수 (기본: 10, 최대: 50) |
| status | String | N | 상태 필터 (CONFIRMED, CANCELLED 등) |
| performanceId | Long | N | 특정 공연 필터 |

**Response:**
```json
{
  "bookings": [
    {
      "bookingId": "550e8400-e29b-41d4-a716-446655440000",
      "bookingNumber": "BK20251225001",
      "status": "CONFIRMED",
      "performanceTitle": "뮤지컬 위키드",
      "scheduleDate": "2025-12-25T14:00:00",
      "userName": "홍*동",
      "userEmail": "ho***@email.com",
      "ticketCount": 2,
      "totalPrice": 150000,
      "createdAt": "2025-12-20T15:30:00"
    }
  ]
}
```

---

## 4. 데이터 모델

### 4.1 Response DTO

```kotlin
// 대시보드 요약
data class DashboardSummaryResponse(
    val period: DatePeriod,
    val totalRevenue: Long,
    val totalBookings: Int,
    val confirmedBookings: Int,
    val cancelledBookings: Int,
    val expiredBookings: Int,
    val averageTicketPrice: Long,
    val totalTicketsSold: Int,
    val comparisonWithPrevious: ComparisonData?
)

// 공연별 매출
data class PerformanceSalesResponse(
    val performanceId: Long,
    val title: String,
    val category: String,
    val venue: String?,
    val period: DatePeriod,
    val totalRevenue: Long,
    val totalBookings: Int,
    val confirmedBookings: Int,
    val cancelledBookings: Int,
    val totalTicketsSold: Int,
    val averageTicketPrice: Long,
    val salesRate: Double
)

// 등급별 매출
data class GradeSalesResponse(
    val grade: SeatGrade,
    val revenue: Long,
    val ticketsSold: Int,
    val averagePrice: Long
)

// 일별 매출
data class DailySalesResponse(
    val date: LocalDate,
    val revenue: Long,
    val bookings: Int,
    val ticketsSold: Int
)
```

---

## 5. 쿼리 설계

### 5.1 공연별 매출 집계 쿼리 (JPQL)

```kotlin
@Query("""
    SELECT new com.github.copyinaction.dashboard.dto.PerformanceSalesProjection(
        p.id,
        p.title,
        p.category,
        v.name,
        p.startDate,
        p.endDate,
        COALESCE(SUM(CASE WHEN b.status = 'CONFIRMED' THEN b.totalPrice ELSE 0 END), 0),
        COUNT(b),
        SUM(CASE WHEN b.status = 'CONFIRMED' THEN 1 ELSE 0 END),
        SUM(CASE WHEN b.status = 'CANCELLED' THEN 1 ELSE 0 END),
        COALESCE(SUM(CASE WHEN b.status = 'CONFIRMED' THEN SIZE(b.bookingSeats) ELSE 0 END), 0)
    )
    FROM Performance p
    LEFT JOIN p.venue v
    LEFT JOIN PerformanceSchedule ps ON ps.performance = p
    LEFT JOIN Booking b ON b.schedule = ps
    WHERE (:startDate IS NULL OR b.createdAt >= :startDate)
      AND (:endDate IS NULL OR b.createdAt <= :endDate)
    GROUP BY p.id, p.title, p.category, v.name, p.startDate, p.endDate
    ORDER BY SUM(CASE WHEN b.status = 'CONFIRMED' THEN b.totalPrice ELSE 0 END) DESC
""")
fun findPerformanceSales(
    @Param("startDate") startDate: LocalDateTime?,
    @Param("endDate") endDate: LocalDateTime?,
    pageable: Pageable
): Page<PerformanceSalesProjection>
```

### 5.2 일별 매출 집계 쿼리

```kotlin
@Query("""
    SELECT new com.github.copyinaction.dashboard.dto.DailySalesProjection(
        CAST(b.createdAt AS LocalDate),
        SUM(CASE WHEN b.status = 'CONFIRMED' THEN b.totalPrice ELSE 0 END),
        COUNT(b),
        SUM(CASE WHEN b.status = 'CONFIRMED' THEN SIZE(b.bookingSeats) ELSE 0 END)
    )
    FROM Booking b
    WHERE b.createdAt >= :startDate AND b.createdAt <= :endDate
      AND (:performanceId IS NULL OR b.schedule.performance.id = :performanceId)
    GROUP BY CAST(b.createdAt AS LocalDate)
    ORDER BY CAST(b.createdAt AS LocalDate)
""")
fun findDailySales(
    @Param("startDate") startDate: LocalDateTime,
    @Param("endDate") endDate: LocalDateTime,
    @Param("performanceId") performanceId: Long?
): List<DailySalesProjection>
```

---

## 6. 보안 및 권한

### 6.1 접근 권한
- 모든 API는 `ROLE_ADMIN` 권한 필요
- `@PreAuthorize("hasRole('ADMIN')")` 적용

### 6.2 데이터 마스킹
- 사용자 이름: `홍길동` → `홍*동`
- 사용자 이메일: `test@email.com` → `te***@email.com`

---

## 7. 성능 고려사항

### 7.1 인덱스 추가 필요
```sql
-- 예매 테이블 인덱스
CREATE INDEX idx_booking_status_created ON booking(booking_status, created_at);
CREATE INDEX idx_booking_schedule_id ON booking(schedule_id);

-- 조회 성능 향상을 위한 복합 인덱스
CREATE INDEX idx_booking_schedule_status ON booking(schedule_id, booking_status);
```

### 7.2 캐싱 전략
- 대시보드 요약: 5분 캐시 (`@Cacheable`)
- 공연별 매출: 1분 캐시
- 실시간 예매: 캐시 없음

### 7.3 페이지네이션
- 모든 목록 API는 페이지네이션 적용
- 기본 페이지 크기: 20
- 최대 페이지 크기: 100

---

## 8. 구현 우선순위

| 우선순위 | API | 이유 |
|----------|-----|------|
| 1 | 대시보드 요약 | 메인 화면 필수 |
| 2 | 공연별 매출 목록 | 핵심 모니터링 기능 |
| 3 | 일별 매출 추이 | 트렌드 분석 |
| 4 | 최근 예매 내역 | 실시간 모니터링 |
| 5 | 공연 상세 매출 | 상세 분석 |
| 6 | 회차별 판매 현황 | 세부 분석 |

---

## 9. 향후 확장 고려

### 9.1 추가 기능 후보
- 시간대별 예매 분석 (피크 타임 파악)
- 좌석 선호도 분석 (Heatmap)
- 취소율 분석
- 사용자 재구매율
- 매출 예측 (ML 기반)

### 9.2 실시간 업데이트
- SSE를 활용한 실시간 대시보드 업데이트
- WebSocket 고려 (양방향 통신 필요 시)
