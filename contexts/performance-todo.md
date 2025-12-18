# 공연/공연장/좌석 선택 구현 TODO

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2025-12-16 | BE | 초안 작성 |
| 1.1 | 2025-12-17 | BE | 예매 안내사항(TicketingNotice) 추가, DDD 리팩토링 섹션 추가, Company CRUD API 완료 체크, Flyway 관련 항목 삭제 |
| 1.2 | 2025-12-17 | BE | Venue/Performance 필드 추가 완료, JSON 기반 좌석 배치도 구현 섹션 추가 |

> 기준 문서: `contexts/performance.md`

---

## 1. 공연장 (Venue) 도메인 ✅

### 엔티티 수정
- [x] `Venue` 엔티티에 `phoneNumber` (대표번호) 필드 추가
- [x] `VenueDto` 요청/응답 DTO에 `phoneNumber` 필드 추가

### 등급별 허용좌석수 테이블 (공연장마다 고정)
- [x] `VenueSeatCapacity` 엔티티 생성
  - venue_id (FK)
  - seatGrade (ENUM: VIP, R, S, A, B)
  - capacity (INT)
- [x] `VenueSeatCapacityRepository` 생성
- [x] `VenueSeatCapacityDto` 생성
- [x] `VenueService`에 좌석 용량 CRUD 로직 추가
- [x] `VenueController`에 좌석 용량 API 추가
  - `GET /api/venues/{venueId}/seat-capacities`
  - `POST /api/venues/{venueId}/seat-capacities`
  - `POST /api/venues/{venueId}/seat-capacities/bulk`
  - `DELETE /api/venues/{venueId}/seat-capacities/{seatGrade}`

---

## 2. 공연 (Performance) 도메인 ✅

### 엔티티 수정
- [x] `Performance` 엔티티에 `actors` (출연진) 필드 추가
- [x] `Performance` 엔티티에 `agency` (기획사), `producer` (제작사), `host` (주최) 필드 추가
- [x] `Performance` 엔티티에 `discountInfo` (할인정보) 필드 추가
- [x] `Performance` 엔티티에 `usageGuide` (이용안내) 필드 추가
- [x] `Performance` 엔티티에 `refundPolicy` (취소/환불규정) 필드 추가
- [x] `Performance` 엔티티에 `detailImageUrl` (상품상세 이미지) 필드 추가
- [x] `PerformanceDto` 요청/응답 DTO에 위 필드들 추가

### 상품/기획사/판매자 정보 연동 ✅
- [x] `Company` 엔티티 생성 (상호, 대표자명, 사업자번호, 연락처 등)
- [x] `CompanyRepository` 생성
- [x] `Performance` 엔티티에 `Company` 연관관계(ManyToOne) 추가 및 `bookingFee`, `shippingGuide` 등 필드 추가
- [x] `Company` 관련 DTO 생성 및 `PerformanceDto` 수정
- [x] `PerformanceService`에서 `Company` 정보 함께 처리하도록 로직 수정
- [x] `Company` 관리용 CRUD API 구현

### 예매 안내사항 (TicketingNotice) - 플랫폼 공통
- [x] `TicketingNotice` 엔티티 생성 (category, title, content, displayOrder, isActive)
- [x] `TicketingNoticeRepository` 생성
- [x] `TicketingNoticeDto` 생성 (Request/Response)
- [x] `TicketingNoticeService` 생성 (CRUD 로직)
- [x] `TicketingNoticeController` 생성 (관리자 CRUD + 사용자 조회 API)

### 이미지 업로드 (MinIO)
- [ ] MinIO 의존성 추가 (`io.minio:minio`)
- [ ] MinIO 설정 추가 (`application.yml`)
- [ ] `MinioConfig` 설정 클래스 생성
- [ ] `FileStorageService` 생성 (업로드/다운로드/삭제)
- [ ] `FileController` 생성 (이미지 업로드 API)
- [ ] 대표 이미지 (썸네일) 업로드 연동
- [ ] 상품상세 이미지 업로드 연동

### 지도 API 연동
- [ ] 지도 API 선택 (Kakao/Naver/Google Maps)
- [ ] `Venue` 엔티티에 `latitude`, `longitude` 필드 추가
- [ ] 프론트엔드용 지도 좌표 응답 API 추가

---

## 3. 공연 회차 (PerformanceSchedule) 도메인 ✅

### 티켓 가격 정책 재설계 (중요) ✅
- [x] **(필수)** `TicketOption`이 `Performance`가 아닌 `PerformanceSchedule`에 연결되도록 엔티티 관계 수정
- [x] 회차별로 좌석 등급과 가격을 설정하는 API 및 로직 구현

### 회차 관리 API
- [ ] 관리자용 회차 CRUD API 확인/보완
  - `POST /api/admin/performances/{performanceId}/schedules`
  - `GET /api/admin/performances/{performanceId}/schedules`
  - `PUT /api/admin/schedules/{scheduleId}`
  - `DELETE /api/admin/schedules/{scheduleId}`
- [ ] 판매 시작일 기반 예매 오픈 로직 검토

### 회차별 좌석 재고 연동
- [ ] `ScheduleTicketStock`과 예매 기능 연동 방식 검토
- [ ] 좌석 재고 자동 차감/복구 로직 확인

---

## 4. 좌석 선택 API

### 달력 모달용 API (신규 개발)
- [ ] `GET /api/performances/{id}/schedules/calendar` API 구현
- [ ] `ScheduleCalendarResponse` DTO 생성
  ```json
  {
    "performanceId": 1,
    "dates": [
      {
        "date": "2025-01-15",
        "schedules": [
          {
            "scheduleId": 10,
            "showTime": "14:00",
            "saleStarted": true,
            "ticketStocks": [
              { "grade": "VIP", "price": 150000, "remaining": 20 },
              { "grade": "R", "price": 100000, "remaining": 45 }
            ]
          }
        ]
      }
    ]
  }
  ```
- [ ] 공연 ID로 전체 회차 조회
- [ ] 날짜별 그룹핑
- [ ] 각 회차의 시간, 등급별 잔여좌석수 포함
- [ ] 판매 시작 여부 표시

### 좌석 선택 규칙 적용 (현재 구현 확인)
- [x] 좌석 점유 시간: 10분
- [x] 최대 선택 좌석수: 4석
- [x] 비회원 예매 차단 로직 추가 (로그인 필수)

---

## 5. DDD 리팩토링 (코드 품질 개선)

### DTO toEntity() 제거 → Domain 팩토리 메서드 전환
- [x] `CreateVenueRequest.toEntity()` → `Venue.create()` (기존 구현 완료)
- [ ] `CreatePerformanceRequest.toEntity()` → `Performance.create()`
- [x] `CreatePerformanceScheduleRequest.toEntity()` → `PerformanceSchedule.create()`
- [x] `CompanyRequest.toEntity()` → `Company.create()`

### 풍부한 도메인 모델 적용
- [ ] Service 비즈니스 로직을 Domain 엔티티로 이동
- [ ] 검증 로직을 Domain 내부로 캡슐화
- [ ] Domain Event 도입 검토

### AuthService 개선
- [ ] 이메일 중복 확인 로직 검토 (Domain Service 도입 여부)

---

## 6. 추후 개발 예정 (1차 범위 제외)

| 기능 | 설명 | 비고 |
|------|------|------|
| 결제 프로세스 | 토스/카카오페이 등 결제 수단 연동 | |
| 예매 취소/환불 | 사용자 예매 취소 및 환불 처리 | |
| 알림 발송 | 예매 확인 이메일/SMS 발송 | |
| 관리자 기능 | 예매 현황 조회, 통계, 정산 | |
| 공연 목록 정렬 | 날짜별/인기도순 정렬 기능 | 요구사항 정의 후 진행 |

---

## 7. JSON 기반 좌석 배치도 구현 (신규)

> 기준 문서: `contexts/seatChat.md`, FE 페이지: `https://next.devhong.cc/seat`

### 설계 개요
- **배치도 JSON** (Venue): FE에서 전달받은 JSON을 그대로 저장/반환 (BE 검증 X)
- **좌석 상태** (ScheduleSeatStatus): 회차별 좌석 상태 관리 (PENDING, RESERVED)
- **FE 전용**: selectedSeats, disabledSeats, mode 등은 FE에서 처리
- **API 분리**: 배치도(캐싱 가능)와 상태(실시간 polling) 별도 API

### 7-1. Venue 엔티티 수정 ✅
- [x] `Venue.seatingChartUrl` → `Venue.seatingChart` (TEXT) 변경
  - JSON을 String으로 저장 (구조 검증 없이 그대로 저장/반환)
- [x] `VenueDto` 수정 (seatingChartUrl → seatingChart)
- [x] `VenueService` 수정

### 7-2. 회차별 좌석 상태 엔티티 생성 ✅
- [x] `SeatStatus` enum 생성
  - `PENDING`: 점유 중 (결제 진행 중, 10분 제한)
  - `RESERVED`: 결제 완료
- [x] `ScheduleSeatStatus` 엔티티 생성
  - schedule_id (FK → PerformanceSchedule)
  - row_num, col_num (좌석 위치, 0부터 시작)
  - status (SeatStatus)
  - held_by (점유 유저 ID, nullable)
  - held_until (점유 만료 시간, nullable)
  - unique constraint: (schedule_id, row_num, col_num)
- [x] `ScheduleSeatStatusRepository` 생성

### 7-3. 좌석 배치도 API 구현 (별도 API - 캐싱 가능) ✅
- [x] `GET /api/venues/{venueId}/seating-chart` - 배치도 JSON 조회
- [x] `PUT /api/venues/{venueId}/seating-chart` - 배치도 저장/수정 (관리자, ADMIN 권한)

### 7-4. 좌석 상태 API 구현 (별도 API - 실시간 polling) ✅
- [x] `GET /api/schedules/{scheduleId}/seat-status` - 회차별 좌석 상태 목록 조회
  - Response: `[{row, col, status}]` (PENDING, RESERVED만 반환)

### 7-5. 좌석 점유/예약 API 구현 ✅
- [x] `POST /api/schedules/{scheduleId}/seats/hold` - 좌석 점유 (10분)
  - Request: `[{row, col}]`
  - 최대 4석 제한
  - 로그인 필수
- [x] `DELETE /api/schedules/{scheduleId}/seats/hold` - 좌석 점유 해제
- [x] `POST /api/schedules/{scheduleId}/seats/reserve` - 좌석 예약 확정 (결제 완료)
- [ ] 점유 만료 스케줄러 구현 (10분 초과 시 자동 해제)

### 7-6. SeatService 구현 ✅
- [x] `SeatService` 생성
  - 좌석 점유/해제/예약 비즈니스 로직
  - [ ] 동시성 제어 (비관적 락 또는 Redis 분산 락) - 추후 구현
  - [x] 점유 만료 처리 (cleanupExpiredHolds 메서드)

---