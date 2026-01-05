# 결제 데이터 수집 시스템 - 구현 TODO List

## 개정이력
| 버전 | 일자 | 작성자 | 내용 |
|------|------|--------|------|
| 1.0 | 2025-01-05 | Claude | 최초 작성 |

---

## Phase 1: Payment 도메인 핵심 구현

### 1.1 엔티티 생성
- [ ] `PaymentMethod` enum 생성 (`payment/domain/PaymentMethod.kt`)
- [ ] `PaymentStatus` enum 생성 (`payment/domain/PaymentStatus.kt`)
- [ ] `Payment` 엔티티 생성 (`payment/domain/Payment.kt`)
- [ ] `PaymentItem` 엔티티 생성 (`payment/domain/PaymentItem.kt`)

### 1.2 Repository 생성
- [ ] `PaymentRepository` 생성 (`payment/repository/PaymentRepository.kt`)
- [ ] `PaymentItemRepository` 생성 (`payment/repository/PaymentItemRepository.kt`)
- [ ] 결제 조회용 커스텀 쿼리 메서드 추가

### 1.3 DTO 생성
- [ ] `PaymentCreateRequest` DTO
- [ ] `PaymentCompleteRequest` DTO (PG 결과 수신)
- [ ] `PaymentCancelRequest` DTO
- [ ] `PaymentResponse` DTO
- [ ] `PaymentDetailResponse` DTO (항목 포함)

### 1.4 Service 구현
- [ ] `PaymentService` 생성 (`payment/service/PaymentService.kt`)
  - [ ] `createPayment()` - 결제 요청 생성
  - [ ] `completePayment()` - 결제 완료 처리
  - [ ] `cancelPayment()` - 결제 취소
  - [ ] `refundPayment()` - 환불 처리
  - [ ] `getPayment()` - 결제 조회
  - [ ] `getPaymentsByUser()` - 사용자 결제 내역

### 1.5 Controller 구현
- [ ] `PaymentController` 생성 (`payment/controller/PaymentController.kt`)
  - [ ] `POST /api/payments` - 결제 요청
  - [ ] `POST /api/payments/{id}/complete` - 결제 완료
  - [ ] `POST /api/payments/{id}/cancel` - 결제 취소
  - [ ] `POST /api/payments/{id}/refund` - 환불
  - [ ] `GET /api/payments/{id}` - 결제 상세
  - [ ] `GET /api/users/me/payments` - 내 결제 내역

### 1.6 이벤트 구현
- [ ] `PaymentRequestedEvent` 생성
- [ ] `PaymentCompletedEvent` 생성
- [ ] `PaymentCancelledEvent` 생성
- [ ] `PaymentRefundedEvent` 생성
- [ ] `PaymentEventHandler` 구현

### 1.7 Booking 연동
- [ ] `Booking` 엔티티에 `Payment` 연결
- [ ] `BookingService.confirmBooking()` → `PaymentService` 연동

### 1.8 테스트
- [ ] `PaymentTest` - 도메인 로직 단위 테스트
- [ ] `PaymentServiceTest` - 서비스 통합 테스트
- [ ] `PaymentControllerTest` - API 테스트

---

## Phase 2: Discount/Coupon 도메인 구현

### 2.1 Discount 엔티티
- [ ] `DiscountType` enum 생성 (`discount/domain/DiscountType.kt`)
- [ ] `PaymentDiscount` 엔티티 생성 (`discount/domain/PaymentDiscount.kt`)
- [ ] `PaymentDiscountRepository` 생성

### 2.2 Coupon 엔티티
- [ ] `DiscountMethod` enum 생성 (`coupon/domain/DiscountMethod.kt`)
- [ ] `UserCouponStatus` enum 생성 (`coupon/domain/UserCouponStatus.kt`)
- [ ] `Coupon` 엔티티 생성 (`coupon/domain/Coupon.kt`)
- [ ] `UserCoupon` 엔티티 생성 (`coupon/domain/UserCoupon.kt`)

### 2.3 Coupon Repository
- [ ] `CouponRepository` 생성
- [ ] `UserCouponRepository` 생성
- [ ] 유효 쿠폰 조회 쿼리 메서드

### 2.4 Coupon DTO
- [ ] `CouponCreateRequest` DTO (관리자용)
- [ ] `CouponResponse` DTO
- [ ] `UserCouponResponse` DTO
- [ ] `CouponValidateRequest` DTO
- [ ] `CouponApplyRequest` DTO

### 2.5 Coupon Service
- [ ] `CouponService` 생성 (`coupon/service/CouponService.kt`)
  - [ ] `createCoupon()` - 쿠폰 생성 (관리자)
  - [ ] `issueCoupon()` - 쿠폰 발급
  - [ ] `validateCoupon()` - 쿠폰 유효성 검증
  - [ ] `applyCoupon()` - 쿠폰 적용
  - [ ] `getUserCoupons()` - 사용자 쿠폰 목록
  - [ ] `restoreCoupon()` - 쿠폰 복구 (결제 취소 시)

### 2.6 Coupon Controller
- [ ] `CouponController` 생성 (`coupon/controller/CouponController.kt`)
  - [ ] `POST /api/admin/coupons` - 쿠폰 생성
  - [ ] `GET /api/admin/coupons` - 쿠폰 목록
  - [ ] `POST /api/coupons/{code}/issue` - 쿠폰 발급
  - [ ] `GET /api/users/me/coupons` - 내 쿠폰 목록
  - [ ] `POST /api/coupons/validate` - 쿠폰 검증
  - [ ] `POST /api/coupons/{id}/apply` - 쿠폰 적용

### 2.7 Payment 연동
- [ ] `Payment.applyDiscount()` 메서드 활용
- [ ] `PaymentService`에 할인 적용 로직 추가
- [ ] 결제 취소 시 쿠폰 복구 로직

### 2.8 스케줄러
- [ ] `CouponExpirationScheduler` - 만료 쿠폰 상태 업데이트

### 2.9 테스트
- [ ] `CouponTest` - 도메인 로직 테스트
- [ ] `CouponServiceTest` - 서비스 테스트
- [ ] `CouponControllerTest` - API 테스트
- [ ] 할인 적용 통합 테스트

---

## Phase 3: 통계/집계 테이블 구현

### 3.1 집계 엔티티
- [ ] `DailySalesStats` 엔티티 생성 (`stats/domain/DailySalesStats.kt`)
- [ ] `PerformanceSalesStats` 엔티티 생성 (`stats/domain/PerformanceSalesStats.kt`)
- [ ] `PaymentMethodStats` 엔티티 생성 (`stats/domain/PaymentMethodStats.kt`)
- [ ] `DiscountStats` 엔티티 생성 (`stats/domain/DiscountStats.kt`)

### 3.2 집계 Repository
- [ ] `DailySalesStatsRepository` 생성
- [ ] `PerformanceSalesStatsRepository` 생성
- [ ] `PaymentMethodStatsRepository` 생성
- [ ] `DiscountStatsRepository` 생성

### 3.3 집계 Service
- [ ] `SalesStatsService` 생성 (`stats/service/SalesStatsService.kt`)
  - [ ] `updateDailySales()` - 일별 매출 업데이트
  - [ ] `updatePerformanceSales()` - 공연별 매출 업데이트
  - [ ] `updatePaymentMethodStats()` - 결제수단 통계 업데이트
  - [ ] `updateDiscountStats()` - 할인 통계 업데이트
  - [ ] `getDailySales()` - 일별 매출 조회
  - [ ] `getPerformanceSales()` - 공연별 매출 조회

### 3.4 이벤트 핸들러 연동
- [ ] `PaymentCompletedEvent` → 집계 테이블 업데이트
- [ ] `PaymentCancelledEvent` → 집계 테이블 업데이트
- [ ] `PaymentRefundedEvent` → 집계 테이블 업데이트

### 3.5 통계 API
- [ ] `StatsController` 생성 (`stats/controller/StatsController.kt`)
  - [ ] `GET /api/admin/stats/daily` - 일별 매출 통계
  - [ ] `GET /api/admin/stats/performance/{id}` - 공연별 매출 통계
  - [ ] `GET /api/admin/stats/payment-methods` - 결제수단별 통계
  - [ ] `GET /api/admin/stats/discounts` - 할인 효과 통계

### 3.6 배치 스케줄러
- [ ] `DailyStatsAggregationScheduler` - 일별 통계 집계 (자정)
- [ ] `StatsRecalculationScheduler` - 통계 재계산 (주 1회)

### 3.7 테스트
- [ ] `SalesStatsServiceTest` - 집계 로직 테스트
- [ ] `StatsControllerTest` - 통계 API 테스트

---

## Phase 4: 고도화 및 성능 최적화

### 4.1 캐싱
- [ ] 통계 API 결과 캐싱 (`@Cacheable`)
- [ ] 쿠폰 유효성 검증 캐싱
- [ ] 캐시 무효화 전략 구현

### 4.2 인덱스 추가
- [ ] Payment 테이블 인덱스 생성 스크립트
- [ ] PaymentItem 테이블 인덱스
- [ ] Coupon 테이블 인덱스
- [ ] 집계 테이블 인덱스

### 4.3 결제 안정화
- [ ] 결제 실패 재시도 로직
- [ ] 결제 타임아웃 처리
- [ ] 중복 결제 방지 (멱등성)
- [ ] PG Webhook 검증

### 4.4 모니터링
- [ ] 결제 실패 알림 (Slack)
- [ ] 일일 매출 리포트 자동 발송
- [ ] Grafana 대시보드 연동

### 4.5 문서화
- [ ] API 문서 (Swagger/OpenAPI)
- [ ] 결제 플로우 시퀀스 다이어그램
- [ ] 운영 가이드 문서

---

## 데이터베이스 마이그레이션

### Flyway 스크립트

```
V2025010501__create_payment_tables.sql
├── payments 테이블
├── payment_items 테이블
└── 인덱스

V2025010502__create_discount_tables.sql
├── payment_discounts 테이블
├── coupons 테이블
├── user_coupons 테이블
└── 인덱스

V2025010503__create_stats_tables.sql
├── daily_sales_stats 테이블
├── performance_sales_stats 테이블
├── payment_method_stats 테이블
├── discount_stats 테이블
└── 인덱스
```

---

## 체크리스트 요약

| Phase | 작업 항목 | 예상 작업량 | 의존성 |
|-------|----------|------------|--------|
| **Phase 1** | Payment 핵심 | 15개 태스크 | Booking |
| **Phase 2** | Discount/Coupon | 18개 태스크 | Phase 1 |
| **Phase 3** | 통계/집계 | 14개 태스크 | Phase 1 |
| **Phase 4** | 고도화 | 10개 태스크 | Phase 1~3 |
| **총계** | - | **57개 태스크** | - |

---

## 우선순위 가이드

### 🔴 Critical (필수)
- Payment 엔티티 및 기본 CRUD
- Booking 연동
- 결제 완료/취소 처리

### 🟠 High (중요)
- PaymentItem (좌석별 가격 기록)
- 쿠폰 시스템
- 일별 매출 집계

### 🟡 Medium (권장)
- 다양한 할인 유형 지원
- 공연별/결제수단별 통계
- 캐싱

### 🟢 Low (선택)
- 고급 분석 기능
- 자동 리포트
- A/B 테스트 할인 그룹
