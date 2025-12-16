# 공연/공연장/좌석 선택 구현 TODO

> 작성일: 2025-12-16
> 기준 문서: `contexts/performance.md`

---

## 1. 공연장 (Venue) 도메인

### 엔티티 수정
- [ ] `Venue` 엔티티에 `phoneNumber` (대표번호) 필드 추가
- [ ] `VenueDto` 요청/응답 DTO에 `phoneNumber` 필드 추가

### 등급별 허용좌석수 테이블 (공연장마다 고정)
- [ ] `VenueSeatCapacity` 엔티티 생성
  - venue_id (FK)
  - seatGrade (ENUM)
  - capacity (INT)
- [ ] `VenueSeatCapacityRepository` 생성
- [ ] `VenueSeatCapacityDto` 생성
- [ ] `VenueService`에 좌석 용량 CRUD 로직 추가
- [ ] `VenueController`에 좌석 용량 API 추가

---

## 2. 공연 (Performance) 도메인

### 엔티티 수정
- [ ] `Performance` 엔티티에 `cast` (출연진) 필드 추가
- [ ] `Performance` 엔티티에 `discountInfo` (할인정보) 필드 추가
- [ ] `Performance` 엔티티에 `usageGuide` (이용안내) 필드 추가
- [ ] `Performance` 엔티티에 `refundPolicy` (취소/환불규정) 필드 추가
- [ ] `Performance` 엔티티에 `detailImageUrl` (상품상세 이미지) 필드 추가
- [ ] `PerformanceDto` 요청/응답 DTO에 위 필드들 추가

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

## 3. 공연 회차 (PerformanceSchedule) 도메인

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
- [ ] 비회원 예매 차단 로직 추가 (로그인 필수)

---

## 5. 추후 개발 예정 (1차 범위 제외)

| 기능 | 설명 | 비고 |
|------|------|------|
| 결제 프로세스 | 토스/카카오페이 등 결제 수단 연동 | |
| 예매 취소/환불 | 사용자 예매 취소 및 환불 처리 | |
| 알림 발송 | 예매 확인 이메일/SMS 발송 | |
| 관리자 기능 | 예매 현황 조회, 통계, 정산 | |
| 공연 목록 정렬 | 날짜별/인기도순 정렬 기능 | 요구사항 정의 후 진행 |

---

## 우선순위 (1차 개발)

| 순위 | 항목 | 이유 |
|------|------|------|
| 1 | 달력 모달용 API | 프론트엔드 예매 플로우 필수 |
| 2 | 비회원 예매 차단 | 로그인 필수 정책 적용 |
| 3 | Performance 필드 추가 | 출연진, 할인정보 등 단순 필드 추가 |
| 4 | Venue 필드 추가 | 대표번호 등 단순 필드 추가 |
| 5 | 회차 관리 API | Admin 기능 보완 |
| 6 | 이미지 업로드 (MinIO) | 인프라 설정 필요 |
| 7 | 등급별 허용좌석수 테이블 | 새 엔티티 추가 필요 |
| 8 | 지도 API 연동 | 외부 API 연동 필요 |

---

## ✅ 확인된 사항 (구현 완료)

- **공연장 관리 (Venue):** CRUD 및 좌석 배치도 기능 구현됨
- **좌석 선택 및 예매:**
  - 회차별 좌석 현황 조회 (실시간)
  - 좌석 임시 선점 및 자동 해제
  - SSE 실시간 좌석 상태 업데이트
  - 선택 좌석 예매 생성
  - 내 예매 목록 조회
- **TicketOption:** Performance에 연결 (공연별 가격 동일)
