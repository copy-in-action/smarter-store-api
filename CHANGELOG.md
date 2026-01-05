# Changelog

## 2026년 1월 5일 (월)

*   **결제 시스템 기본 도메인 및 API 구축 (Phase 1) [CCS-125]:**
    *   **설계 문서 추가**: 결제 데이터 수집 시스템 설계 및 TODO 문서 구축.
    *   **Rich Domain Model 적용**: 결제 상태 변경 로직(`complete`, `cancel`, `refund`)을 `Payment` 엔티티 내부에 캡슐화하여 데이터 정합성 강화.
    *   **데이터 정합성 강화**: `BookingSeat` 정보를 기반으로 결제 시점의 좌석 정보(가격, 등급, 위치)를 담은 `PaymentItem` 스냅샷을 자동 생성하도록 구현.
    *   **RESTful API 구현**: 결제 요청, 완료 승인, 취소, 상세 조회 엔드포인트 구축 및 Swagger 문서화.
    *   **TDD 기반 검증**: 도메인 및 서비스 레이어 단위 테스트(`PaymentTest`, `PaymentServiceTest`)를 통한 비즈니스 규칙 검증 완료.

*   **할인 및 쿠폰 시스템 구축 (Phase 2) [CCS-123]:**
    *   **FE 제안 완벽 수용**: `PaymentCreateRequest` DTO 내에 약관 동의(`isAgreed`) 필드 추가 및 `discounts` 필드로 모든 할인 유형(쿠폰, 포인트 등)을 통합 관리하도록 설계.
    *   **쿠폰 서비스 일괄 연동**: 리팩토링된 `CouponService`의 일괄 처리 인터페이스(`useCoupons`, `restoreCoupons`)를 연동하여 성능 최적화 및 원자적 처리 보장.
    *   **금액 검증 엔진 고도화**: `Payment.validateAmount` 도메인 메서드 도입을 통해 클라이언트 계산 금액과 서버 정책 금액 간의 불일치를 원천 차단.
    *   **쿠폰 API 구현**:
        *   사용자용: 쿠폰 발급(`POST /api/coupons/issue`), 내 쿠폰 목록(`GET /api/coupons/me`), 쿠폰 유효성 검증(`POST /api/coupons/validate`)
        *   관리자용: 쿠폰 생성(`POST /api/admin/coupons`), 목록 조회(`GET /api/admin/coupons`), 상세 조회(`GET /api/admin/coupons/{id}`), 비활성화(`PATCH /api/admin/coupons/{id}/deactivate`)

*   **통계 및 정산 시스템 구축 (Phase 3) [CCS-107, CCS-127]:**
    *   **실시간 매출 집계 엔진 [CCS-107]**: 결제 완료/취소/환불 이벤트를 구독하여 일별, 공연별, 결제수단별, 할인별 통계를 실시간으로 집계하는 비동기 파이프라인 구축.
    *   **통계 전용 도메인 및 Repository [CCS-107]**: `DailySalesStats`, `PerformanceSalesStats`, `PaymentMethodStats`, `DiscountStats` 4종의 통계 엔티티 및 비관적 락(`PESSIMISTIC_WRITE`)을 적용한 고성능 집계 저장소 구현.
    *   **관리자용 통계 조회 API [CCS-127]**: 일별 매출(`/daily`), 공연별(`/performance/{id}`), 결제수단별(`/payment-methods`), 할인별(`/discounts`) 4종의 관리자 전용 REST API 개발.
    *   **통계 배치 스케줄러 [CCS-107]**: 일별 통계 재집계(`DailyStatsAggregationScheduler`, 매일 자정) 및 주간 통계 재계산(`StatsRecalculationScheduler`, 매주 일요일 03:00) 스케줄러 구현.
    *   **스케줄러 공통 패키지 통합**: 도메인별로 분산된 스케줄러를 `common/scheduler/` 패키지로 통합하여 유지보수성 향상.
    *   **스케줄러 에러 알림 체계**: `STATS_DAILY_AGGREGATION_FAILED`, `STATS_RECALCULATION_FAILED` ErrorCode 추가 및 `LogLevel.ERROR` 적용으로 Slack 알림 연동.
    *   **도메인 테스트 작성**: 4종의 통계 엔티티에 대한 단위 테스트(`DailySalesStatsTest`, `PerformanceSalesStatsTest`, `PaymentMethodStatsTest`, `DiscountStatsTest`) 구현.

*   **공통 Enum 스키마 및 조회 API 추가 [CCS-124]:**
    *   **Enum에 `@Schema(enumAsRef = true)` 적용:** 모든 API 응답에서 사용되는 Enum이 OpenAPI에서 `$ref`로 참조되어 Orval에서 단일 타입으로 생성되도록 개선
        *   적용 대상: `SeatGrade`, `BookingStatus`, `SeatStatus`, `NoticeCategory`, `Role`, `AuditCategory`, `AuditAction`, `AuditTargetType`, `SeatEventAction`
    *   **Enum 조회 API 구현:** `GET /api/enums` - 전체 Enum을 한번에 조회 (인증 불필요)
        *   `EnumResponse` DTO: `code`(영문)와 `label`(한글) 필드 포함
        *   프론트엔드에서 앱 초기화 시 호출하여 캐싱해서 사용
    *   **Swagger Tag kebab-case 통일:** `Admin Dashboard` → `admin-dashboard`, `Enums` → `enums`

## 2026년 1월 4일 (일)

*   **SSE 초기 데이터 통합 (Race Condition 해결):**
    *   SSE 연결 시 `snapshot` 이벤트로 현재 좌석 상태(pending/reserved)를 즉시 전송하도록 개선
    *   REST API(`GET /api/schedules/{id}/seat-status`)와 SSE 구독 사이의 데이터 누락 문제 해결
    *   `SseService`에 `SeatService` 의존성 추가 및 `sendSnapshot()` 메서드 구현
    *   REST API deprecated 처리 및 SSE 사용 유도 안내 추가
    *   Swagger 문서에 SSE 이벤트별 응답 샘플 추가 (프론트엔드 개발 가이드)
*   **좌석 도메인/DTO 리팩토링:**
    *   `BookingSeat` 엔티티: `rowName(String)` → `row(Int)`, `seatNumber` → `col` 변경
    *   `BookingSeatResponse`: `grade`, `price` 필드 삭제, `row`/`col` 필드명 통일
    *   `ScheduleSeatStatusResponse`: `pending`/`reserved`를 `List<String>` → `List<SeatPositionResponse>` 구조로 변경
    *   관련 서비스(`BookingService`, `BookingCleanupScheduler`, `SeatService`) 수정
    *   **BREAKING CHANGE**: `booking_seat` 테이블 스키마 변경 필요 (테이블 삭제 후 자동 생성)
*   **SeatingChartParser 단순화:**
    *   현재 JSON 구조(`seatTypes.{grade}.positions`)에 맞게 파싱 로직 전면 재설계
    *   레거시 구조(`seatGrades` 배열) 지원 코드 제거
    *   `Missing 'seatGrades' field` 경고 및 등급 검증 실패 버그 수정

## 2026년 1월 2일 (금)

*   **DDD 아키텍처 고도화 및 풍부한 도메인 모델(Rich Domain Model) 적용:**
    *   **도메인 로직 이동:** 서비스 계층에 흩어져 있던 핵심 비즈니스 계산 로직을 도메인 엔티티 및 값 객체(VO)로 이동하여 응집도를 높였습니다.
        *   좌석 변경 계산 로직: `SeatOccupationService` → `SeatSelection` (VO) 이동
        *   잔여 좌석 계산 로직: `PerformanceScheduleService` → `PerformanceSchedule` 엔티티 이동
        *   예매 번호 생성 규칙: `BookingService` → `Booking` 엔티티 이동
    *   **Domain Factory 패턴 적용:** DTO가 엔티티 생성 방식을 결정하지 않도록, 엔티티 내부 팩토리 메서드와 Command 객체를 도입했습니다. (`TicketOptionCommand`, `SeatCapacityCommand`)
*   **이벤트 기반 아키텍처 (EDD) 도입을 통한 결합도 해소:**
    *   **도메인 이벤트 발행:** Spring Data JPA의 `@DomainEvents`를 활용하여 애그리거트 상태 변경 시 이벤트를 발행하도록 개선했습니다.
    *   **서비스 간 느슨한 결합:** `BookingService`와 `VenueService`가 다른 도메인 서비스를 직접 호출하던 강한 의존성을 제거하고, 이벤트 핸들러(`BookingEventHandler`, `VenueEventHandler`)를 통해 비동기/격리 처리가 가능하도록 구조를 개편했습니다.
    *   **공통 인프라 구축:** 모든 엔티티가 이벤트를 관리할 수 있도록 `BaseEntity`에 도메인 이벤트 발행 기능을 통합했습니다.
*   **전역 검증(Validation) 전략 수립 및 적용:**
    *   **역할 분담 명확화:** DTO(형식 및 포맷 검증)와 Domain(비즈니스 논리 및 무결성 검증)의 책임을 명확히 구분하는 원칙을 수립하고 `DEVELOPMENT_CONVENTION.md`에 명시했습니다.
    *   **도메인 검증 강화:** `Performance`, `PerformanceSchedule` 등에 날짜 순서 및 업무 규칙 검증 로직(`validate()`)을 추가하여 "언제나 올바른 상태의 엔티티"를 보장하도록 했습니다.
    *   **DTO 검증 보강:** `CompanyRequest`, `PerformanceRequest` 등에 Bean Validation(@Pattern, @Email, @Min)을 추가하여 잘못된 형식을 입구에서 차단하도록 했습니다.
*   **엔티티 명명 규칙 및 DB 예약어 대응 리팩토링:**
    *   `Booking` 엔티티의 필드명을 컨벤션에 맞춰 리팩토링했습니다. (`status` → `bookingStatus`, `user` → `siteUser`)
    *   이로 인해 발생한 대시보드 통계 쿼리(JPQL) 및 관련 서비스 로직을 전수 조사하여 업데이트를 완료했습니다.
*   **문서 및 DX(Developer Experience) 개선:**
    *   **설계 문서 추가:** `이벤트_기반_좌석_처리.md`를 작성하여 신규 도입된 아키텍처를 시각화하고 설명했습니다.
    *   **Swagger 예제 최신화:** 테스트 편의를 위해 공연 회차 생성 예제 날짜를 현재 시점(2026년 1월)으로 업데이트했습니다.
*   **SSE Broken pipe 에러 처리 개선:**
    *   `GlobalExceptionHandler`에 `IOException` 전용 핸들러 추가
    *   클라이언트 연결 끊김(Broken pipe) 시 ERROR 대신 DEBUG 레벨로 로깅
    *   불필요한 에러 로그 노이즈 감소
*   **GitHub 배포 Slack 알림 기능 추가 (CCS-120):**
    *   배포 시작/성공/실패 시 Slack 알림 발송
    *   알림 타이밍을 deploy → build-and-push 단계로 변경
    *   변경된 모든 커밋 내역을 알림에 표시하는 기능 추가
*   **Critical 에러 코드 추가 (Slack 알림 대상):**
    *   `DATA_INTEGRITY_ERROR`: 데이터 정합성 오류 발생 시 (점유되지 않은 좌석 확정 시도 등)
    *   `BOOKING_CONFIRM_FAILED`: 예매 확정 처리 중 오류 발생 시
    *   `EXTERNAL_SERVICE_ERROR`: 외부 서비스 연동 오류 (추후 결제 연동 시 사용)
    *   LogLevel.ERROR 설정으로 Slack 알림 대상에 포함
*   **예매 시스템 코드 가이드 문서 추가:**
    *   핵심 파일 6개 및 아키텍처 다이어그램 포함
    *   상태 전이, 동시성 처리, SSE 동기화 설명
    *   테스트 시나리오 및 디버깅 가이드 추가

## 2025년 12월 31일 (수)

*   **관리자 매출 대시보드:** API 6개 + HTML 페이지 (`/api/admin/dashboard/*`)
    *   요약, 공연별/회차별 매출, 일별 추이, 최근 예매 내역 조회
    *   Chart.js 기반 시각화 (라인/도넛 차트)
*   **ErrorCode 로그레벨:** `logLevel` 속성 추가 (ERROR/WARN/DEBUG)
    *   인증 관련 빈번한 예외는 DEBUG로 분류하여 로그 노이즈 감소
*   **Docker Compose:** 로컬용 PostgreSQL 컨테이너 추가
    *   health check 기반 의존성 관리로 DB 준비 후 앱 시작
*   **모니터링 폴더 정리:** `monitoring/` → `monitoring-local/`, `monitoring-prod/` 분리
*   **회차 조회 API:** `GET /api/schedules/{scheduleId}` 추가
    *   예매 시 선택한 회차의 등급별 가격 및 잔여석 조회
*   **컨트롤러 통합:** `SeatController` → `ScheduleController` 통합
    *   `/api/schedules/{scheduleId}/*` 경로를 단일 컨트롤러에서 관리
*   **문서:** Grafana 사용 매뉴얼 작성
*   **DDD 리팩토링:** Service 책임 분리
    *   AuthService → TokenService, EmailVerificationService 분리
    *   BookingService → SeatOccupationService 분리
    *   VenueService → TicketOptionSyncService 분리 (Aggregate 경계 준수)
*   **로그 정리:** 프로젝트 전체 로그 검토 및 개선
    *   불필요한 디버그 로그 삭제 (JwtAuthenticationFilter, CookieService, EmailService)
    *   NoResourceFoundException 핸들러 추가 (favicon.ico 등 DEBUG 처리)
    *   BookingCleanupScheduler 중복 로그 제거
    *   미사용 ErrorCode 17개 삭제
*   **개인정보 보호 강화:**
    *   로그아웃 시 Refresh Token DB에서 삭제
    *   전화번호 AES-256-GCM 암호화 (`EncryptionUtil`, `EncryptedStringConverter`)
    *   기존 평문 전화번호 마이그레이션 스크립트 (`PhoneNumberMigration`)
    *   감사로그 마스킹 범위 확대 (email, phoneNumber, otp, token 추가)
    *   감사로그 응답 이메일 마스킹 (`u***r@example.com`)
*   **인증 API 개선:**
    *   `UserResponse`에 `phoneNumber` 필드 추가
    *   `/refresh` API 쿠키 기반으로 변경 (`RefreshTokenRequest` DTO 삭제)

## 2025년 12월 29일 (월)

*   **문서 관리 체계 개선 (Documentation Refactoring):**
    *   **폴더 구조 전면 개편:** 문서의 목적과 대상에 따라 `01_개발_컨벤션`, `02_시스템_아키텍처`, `03_기능_명세`, `04_기술_노트`, `05_인프라_및_배포`로 폴더 구조를 재정비하여 접근성을 향상시켰습니다.
    *   **한글화 및 표준화:** 모든 문서의 파일명을 한글로 통일하고, 상단에 **개정이력(Revision History)** 섹션을 필수화하여 문서의 변경 이력을 체계적으로 관리하도록 개선했습니다.
    *   **기술 노트 신설:** 아키텍처 설계와 구분되는 특정 기술 이슈 해결 사례(예: Kotlin 마스킹 최적화)를 `04_기술_노트`로 분리하여 기술 자산화했습니다.
*   **예매 시스템 좌석 인덱스 기준 변경:**
    *   **사용자 편의성 개선 (1-based index 도입):** 클라이언트가 좌석을 선택할 때 더 직관적으로 사용할 수 있도록 `StartBookingRequest`의 행(row)과 열(col) 좌표 기준을 기존 0부터 시작에서 **1부터 시작**하도록 변경했습니다.
    *   **데이터 매핑 로직 적용:** API 요청으로 들어온 1-based 인덱스를 서버 내부에서 기존의 0-based 데이터(좌석 배치도, DB 상태 등)와 정확히 매핑되도록 처리하여 데이터 정합성 문제를 방지했습니다.
*   **공연 회차 조회 API 기능 강화:**
    *   **좌석 등급별 가격 정보 제공:** 예매 프로세스에서 필요한 가격 정보를 클라이언트가 쉽게 확인할 수 있도록, 특정 날짜의 회차 목록 조회 API(`/api/performances/{id}/schedules`) 응답에 등급별 **가격(`price`)** 필드를 추가했습니다. (`TicketOptionWithRemainingSeatsResponse`)
*   **Audit 로그 시스템 구현:**
    *   **AOP 기반 자동 감사 로그:** `@Auditable` 어노테이션을 사용하여 API 호출 시 자동으로 감사 로그를 기록하는 시스템을 구현했습니다.
        *   `AuditAspect`: `@Around` advice로 메서드 실행 전후 로깅
        *   `AuditLogService`: 비동기(`@Async`) 로그 저장, 민감정보 마스킹(password, cardNumber 등)
        *   `AuditLog` 엔티티: userId, action, category, targetType, targetId, requestPath, ipAddress 등 기록
    *   **Enum 정의:** `AuditCategory`, `AuditAction` (30개), `AuditTargetType`
    *   **적용된 API:** 예매, 인증, 관리자용 공연/회차/공연장/공지사항 CRUD
    *   **관리자 조회/통계 API 구현:** 필터 기반 목록 조회, 상세 조회, 사용자별 조회, 기간별 통계
    *   **API 문서화 및 가시성 개선:**
        *   **Swagger 문서 보강:** `@Auditable`이 적용된 모든 API의 설명에 **"[Audit Log] 이 작업은 감사 로그에 기록됩니다."** 문구를 추가하여 보안 감사 대상임을 명확히 안내했습니다.
        *   **관리자 편의성 증대:** `AuditLogResponse` DTO에 HTTP 상태 코드(200, 400 등)를 관리자가 이해하기 쉬운 한글 설명("성공", "요청 오류", "권한 없음" 등)으로 변환한 `responseStatusDescription` 필드를 추가했습니다.
    *   **설계 문서:** `documents/design/Audit_로그_설계.md` 작성 및 구현 현황 업데이트

## 2025년 12월 27일 (토)

*   **API 날짜 포맷 원복 (ISO 8601):**
    *   글로벌 서비스 확장을 고려하여, 이전 커밋에서 변경했던 공연 회차 API(`PerformanceScheduleDto`)의 날짜 포맷(`yyyy-MM-dd HH:mm`)을 제거하고 ISO 8601 표준 포맷을 유지하도록 원복했습니다.
*   **좌석 상태 조회 응답 구조 개선:**
    *   `GET /api/schedules/{scheduleId}/seat-status` API의 응답 구조를 변경했습니다.
    *   `pending` 및 `reserved` 상태별로 좌석 위치(`row,col`) 리스트를 그룹화하여 반환하도록 수정하여 프론트엔드 렌더링 효율성을 개선했습니다.
*   **공연 시간 데이터 정규화:**
    *   중복 스케줄 체크의 정확도를 높이고 데이터 일관성을 확보하기 위해, 공연 회차 등록/수정 시 초(second) 및 나노초(nano) 단위를 자동으로 절삭(`00`)하여 저장하도록 개선했습니다.
*   **예매 존재 시 공연 회차 수정/삭제 제한:**
    *   데이터 무결성과 운영 안정성을 위해, 이미 예매(`PENDING`, `CONFIRMED`)가 진행된 공연 회차에 대해서는 정보 수정 및 삭제를 제한하는 로직을 추가했습니다. (`PerformanceScheduleService.updateSchedule`, `deleteSchedule`)
*   **코드 리팩토링 및 DDD 아키텍처 개선:**
    *   **BookingService 리팩토링:** 비대한 트랜잭션 스크립트 형태의 `startBooking` 메서드를 비즈니스 흐름별(좌석 계산, DB 반영, 예매 생성)로 분리하여 가독성을 높였습니다.
    *   **Booking 엔티티 강화:** `addSeats` 메서드를 추가하여 `BookingSeat` 생성 로직을 도메인 내부로 캡슐화하고, `section` 하드코딩을 상수로 개선했습니다.
    *   **PerformanceSchedule Aggregate Root 강화:** 티켓 옵션(`TicketOption`)의 생명주기 관리를 서비스에서 엔티티로 위임(Cascade, OrphanRemoval)하여 데이터 정합성을 높였습니다.
    *   **도메인 로직 이동:** 서비스 계층에 산재하던 좌석 등급 검증 로직을 `SeatingChartParser`로 통합하여 응집도를 개선했습니다.

## 2025년 12월 26일 (금)

*   **공연 회차 관리 개선:**
    *   **중복 스케줄 생성 방지:** `PerformanceScheduleService` 및 `PerformanceScheduleRepository`에 중복된 공연 회차 생성/수정을 방지하는 로직을 추가하여 데이터 무결성을 강화했습니다.
    *   **에러 처리 강화:** `ErrorCode.DUPLICATE_SCHEDULE`을 추가하여 중복 스케줄 발생 시 명확한 에러 메시지를 반환하도록 했습니다.
*   **문서 업데이트:**
    *   `contexts/actorsBySchedule.md` 업데이트: 공연별 및 회차별 출연진 관리 구조 설계를 보완하고, 효율적인 배우별 조회 전략을 제시했습니다.

*   **좌석 시스템 성능 개선 및 아키텍처 리팩토링:**
    *   **잔여 좌석 계산 방식 최적화 (Read-time → Write-time):**
        *   `TicketOption` 엔티티에 `totalQuantity` 필드를 추가하여 회차 생성 시 등급별 총 좌석 수를 미리 계산하여 저장하도록 개선했습니다.
        *   조회 요청마다 수행되던 무거운 JSON 파싱 로직을 제거하여 API 응답 속도를 대폭 향상시키고 서버 부하를 줄였습니다.
    *   **`SeatingChartParser` 전면 개편:**
        *   실제 공연장 데이터 포맷(`seatTypes` + `position` 기반)에 맞춰 파싱 로직을 완전히 재설계했습니다.
        *   파싱 과정의 투명성을 위해 상세 로깅 시스템을 도입하여 데이터 정합성 확인을 용이하게 했습니다.
*   **공지사항(Notice) 도메인 리팩토링:**
    *   **명칭 및 경로 간소화:** `TicketingNotice`를 `Notice`로 전면 변경하고, API 경로를 `/api/notices`로 축소하여 가독성과 범용성을 높였습니다.
    *   **컨트롤러 분리:** 관리자 전용 API를 `AdminNoticeController`로 분리하여 보안 정책(Role-based Access Control)을 강화했습니다.
*   **프론트엔드 협업 및 DX(Developer Experience) 강화:**
    *   **Enum Swagger 문서화:** `SeatGrade`, `NoticeCategory`, `BookingStatus` 등 주요 Enum에 `@Schema`와 한글 설명을 추가하여 프론트엔드에서 API 스펙을 쉽게 파악할 수 있도록 개선했습니다.
    *   **Enum 파싱 에러 메시지 개선:** 잘못된 Enum 값을 요청 본문에 포함할 경우, 500 에러 대신 400 Bad Request와 함께 **"허용된 값 목록"**을 포함한 친절한 상세 메시지를 반환하도록 `GlobalExceptionHandler`를 강화했습니다.
*   **코드 정리 및 보안:**
    *   **미사용 코드 제거:** 지정 좌석제 도입으로 불필요해진 `ScheduleTicketStock` 관련 로직과 에러 코드를 모두 삭제했습니다.
    *   **관리자 API 가이드라인 수립:** `/api/admin/**` 경로 규칙 및 패키지 구조 원칙을 `contexts/controller-auth.md`에 명시했습니다.
*   **데이터베이스 안정화 및 마이그레이션:**
    *   **DDL 에러 해결:** `ScheduleSeatStatus` 및 `TicketOption` 테이블의 신규 컬럼/인덱스 생성 시 발생하던 데이터 마이그레이션 이슈를 해결했습니다.
    *   **기본값 보장:** `totalQuantity` 컬럼에 `@ColumnDefault("0")`를 적용하여 기존 데이터와의 호환성을 확보했습니다.
*   **문서화:**
    *   **좌석 시스템 개선 제안서 작성 (`좌석_시스템_개선_제안.md`):** 아키텍처 변경 배경과 성능 기대 효과를 문서화했습니다.

## 2025년 12월 24일 (수)

*   **사용자용 공연 일정 조회 API 구현:**
    *   **예매 가능 날짜 목록 조회 API 추가 (`GET /api/performances/{id}/schedules/dates`):** 티켓 판매가 시작되고 아직 공연 전인 회차들의 날짜 목록을 반환하는 기능.
    *   **특정 날짜의 예매 가능 회차 조회 API 추가 (`GET /api/performances/{id}/schedules`):** 선택한 날짜의 회차 정보와 각 좌석 등급별 잔여석 수를 실시간으로 계산하여 반환하는 기능.
*   **도메인 이전(`ticket-api.devhong.cc`) 영향도 분석:**
    *   `CookieService`의 쿠키 도메인 설정(`.devhong.cc`) 검토 및 세션 유지 정상 작동 확인.
    *   SSL 인증서 신규 발급 및 프론트엔드 API Base URL 업데이트 체크리스트 도출.
    *   CORS_ALLOWED_ORIGINS 업데이트 `api.ticket.devhong.cc` -> `ticket-api.devhong.cc`

## 2025년 12월 23일 (화)

*   **좌석 일괄 점유 및 SSE 실시간 동기화 시스템 구현:**
    *   **Phase 1 - SSE 인프라 구축:**
        *   `SeatEventMessage`, `SeatPosition`, `SeatEventAction` SSE 이벤트 DTO 정의 (`SeatDto.kt`)
        *   `SseService` 개발: `ConcurrentHashMap` 기반 Emitter 관리, 45초 Heartbeat, 이벤트 전송 인터페이스
        *   `SeatController`에 SSE 구독 엔드포인트 추가 (`GET /api/schedules/{id}/seats/stream`)
        *   `SecurityConfig` 업데이트: SSE 및 좌석 상태 조회 경로 permitAll 설정
    *   **Phase 2 - BookingService 리팩토링 (일괄 점유 적용):**
        *   `startBooking` API 수정: 좌석 목록(`List<SeatPositionRequest>`)을 인자로 받아 일괄 점유 처리
        *   DB Unique 제약 조건을 활용한 동시성 제어 및 `DataIntegrityViolationException` 예외 처리
        *   점유 성공 시 `SseService`를 통해 `OCCUPIED` 이벤트 발행
        *   기존 개별 좌석 선택(`selectSeat`) 및 해제(`deselectSeat`) API 제거 (BookingController, BookingService)
        *   `SeatController`의 `holdSeats`, `releaseSeats`, `reserveSeats` API 제거 (중복 기능)
        *   `SeatService`의 불필요한 메서드 정리
    *   **Phase 3 - 라이프사이클 관리:**
        *   `confirmBooking` 시 `CONFIRMED` SSE 이벤트 발행 로직 추가
        *   `cancelBooking` 및 `BookingCleanupScheduler` 만료 처리 시 좌석 해제 및 `RELEASED` 이벤트 발행
*   **동일 사용자 좌석 재선택 시 불필요한 이벤트 발행 버그 수정:**
    *   기존: 좌석 재선택 시 모든 좌석에 대해 RELEASED → OCCUPIED 이벤트 발행
    *   수정: 차등 좌석 처리 로직 구현 (`keptSeats`, `releasedSeats`, `addedSeats`)
        *   `keptSeats` (교집합): 만료시간만 연장, SSE 이벤트 없음
        *   `releasedSeats` (기존 - 신규): DB 삭제, RELEASED 이벤트 발행
        *   `addedSeats` (신규 - 기존): DB 생성, OCCUPIED 이벤트 발행
    *   Race condition 방지를 위해 유지될 좌석은 삭제/재생성 없이 `extendHold()` 호출

## 2025년 12월 22일 (월)

*   **Slack 에러 알림 기능 추가:**
    *   `logback-slack-appender`를 통한 실시간 에러 로그 Slack 전송 기능 구현.
    *   `prod` 프로파일에서만 Slack 알림이 활성화되도록 Logback 설정 (`<springProfile>`) 추가.
    *   환경 변수(`SLACK_WEBHOOK_URL`) 미설정 시 구동 에러 방지를 위한 기본값 처리.
    *   `application.yml`의 `mail` 설정 위치가 잘못되었던 구조적 결함 수정.
*   **좌석 관련 필드명 및 매핑 일관성 강화:**
    *   `ScheduleSeatStatus` 엔티티의 `status` 필드명을 `seatStatus`로 변경하고, 관련된 모든 서비스 로직, DTO, Repository 쿼리 메서드를 업데이트했습니다.
    *   `SeatController`에서 `AuthenticationPrincipal`을 `UserDetails` 대신 `CustomUserDetails` 타입으로 직접 받아 `user.id`에 접근하도록 수정하여 `NumberFormatException`을 해결하고 타입 안정성을 높였습니다.
    *   `SeatController`에 `CustomUserDetails` 임포트 누락 및 `response` 변수 미선언 오류를 수정했습니다.
    *   데이터베이스 스키마와 엔티티 필드명 불일치로 인한 `PSQLException` 에러를 해결하기 위해, `ddl-auto: update` 설정을 활용하여 `schedule_seat_status` 테이블을 재정의하도록 유도했습니다.

*   **5분 타이머 기반 좌석 예매 시스템 구현:**
    *   Booking 도메인 추가 (`Booking`, `BookingSeat`, `BookingStatus`, `SeatLock`)
    *   UUID 기반 예매 ID, 5분 타이머 서버 측 관리
    *   `SeatLock` 테이블의 Unique 제약조건으로 좌석 동시 선택 방지
    *   예매 API 구현 (`/api/bookings`)
        *   `POST /start`: 예매 시작 (5분 타이머 가동)
        *   `POST /{id}/seats`: 좌석 선택 (점유)
        *   `DELETE /{id}/seats`: 좌석 선택 취소
        *   `GET /{id}/time`: 남은 시간 조회
        *   `POST /{id}/confirm`: 예매 확정
        *   `DELETE /{id}`: 예매 취소
    *   DTO 추가: `StartBookingRequest`, `SeatRequest`, `BookingResponse`, `BookingSeatResponse`, `BookingTimeResponse`
    *   Repository 추가: `BookingRepository`, `BookingSeatRepository`, `SeatLockRepository`
*   **만료 예매 자동 정리 스케줄러 추가:**
    *   `BookingCleanupScheduler`: 1분마다 만료된 PENDING 예매를 EXPIRED로 변경
    *   `@EnableScheduling` 어노테이션 추가 (`SmarterStoreApiApplication`)
*   **ErrorCode 업데이트:**
    *   Booking 관련 에러코드 추가: `BOOKING_NOT_FOUND`, `BOOKING_EXPIRED`, `BOOKING_INVALID_STATUS`, `PRICE_MISMATCH`
    *   공통 에러코드 추가: `FORBIDDEN`, `INVALID_REQUEST`
    *   미사용 Reservation 에러코드 제거: `RESERVATION_NOT_FOUND`, `RESERVATION_ALREADY_CANCELLED`, `RESERVATION_CANNOT_CONFIRM`
*   **문서 추가:**
    *   `좌석_예매_테스트_워크플로우.md`: FE/BE 통합 테스트 가이드 (API 요청/응답 예시, 에러 케이스, 엣지 케이스, cURL 테스트 스크립트 포함)
    *   `좌석_예매_시스템.md` v2.0 업데이트: 5분 타이머 기반 재설계 반영

## 2025년 12월 21일 (일)

*   **공연장 삭제 시 검증 로직 추가:**
    *   공연이 등록된 공연장 삭제 시 409 Conflict 에러 반환
    *   `ErrorCode.VENUE_HAS_PERFORMANCES` 추가: "등록된 공연이 있는 공연장은 삭제할 수 없습니다."
    *   `PerformanceRepository.existsByVenueId()` 메서드 추가
    *   `VenueService.deleteVenue()`에 검증 로직 추가
*   **Swagger UI 정렬 설정:**
    *   태그(API 그룹) 및 API 경로 알파벳순 정렬 (`application.yml`에 `springdoc.swagger-ui.tags-sorter`, `operations-sorter` 설정)
    *   Schemas 알파벳순 정렬 (`OpenApiConfig`에 `OpenApiCustomizer` 추가)
*   **공연 회차 날짜 검증 완화:**
    *   `CreatePerformanceScheduleRequest`, `UpdatePerformanceScheduleRequest`에서 `@Future` 어노테이션 제거
    *   과거 날짜로도 공연 회차 등록/수정 가능
*   **Company 용어 통일:**
    *   "기획사/판매자" → "판매자"로 주석 및 메시지 통일
    *   변경 파일: `CompanyController`, `Company`, `CompanyDto`, `ErrorCode`

## 2025년 12월 19일 (금)

*   **DDD Rich Domain Model 적용 및 버그 수정:**
    *   `Venue` 엔티티를 풍부한 도메인 모델(Aggregate Root)로 리팩토링하여 `VenueSeatCapacity`의 생명주기를 직접 관리하도록 개선했습니다.
    *   `Venue.updateSeatingChart` 메서드 내에 변경 감지(Dirty Checking) 로직을 구현하여, 무조건적인 삭제 후 재등록(Delete-Insert)으로 인한 Unique Constraint 위반 문제를 해결했습니다.
    *   `VenueService`에서 불필요한 Repository 의존성(`VenueSeatCapacityRepository`)을 제거하고, 도메인 엔티티에 비즈니스 로직을 위임하도록 수정했습니다.
    *   `VenueSeatCapacity` 엔티티에서 사용하지 않는 `updateCapacity` 메서드를 제거했습니다.

## 2025년 12월 18일 (목)

*   **좌석 배치도 API 통합 및 개선:**
    *   `PUT /api/venues/{id}/seating-chart` API에서 배치도와 등급별 좌석수를 한 번에 저장하도록 통합
    *   `seatingChart` 타입을 String에서 JSON Object로 변경 (프론트 편의성 개선, 이스케이프 불필요)
    *   등급별 좌석수 별도 CRUD API 제거 (배치도 저장 API에 통합)
        *   삭제된 API: `GET/POST /venues/{id}/seat-capacities`, `POST /venues/{id}/seat-capacities/bulk`, `DELETE /venues/{id}/seat-capacities/{grade}`
    *   `VenueService`에 ObjectMapper 주입하여 JSON 직렬화/역직렬화 처리
    *   `VenueSeatCapacityBulkRequest` DTO 삭제
*   **VenueSeatCapacity CRUD 구현:**
    *   `VenueService`, `VenueController`에 등급별 좌석 용량 API 추가 (조회, 단건등록, 일괄설정, 삭제) → 이후 통합 API로 대체됨
    *   `VenueSeatCapacity.create()` 팩토리 메서드 추가
*   **DDD 리팩토링 - DTO toEntity() 제거:**
    *   `PerformanceSchedule.create()` 팩토리 메서드 구현
    *   `CreatePerformanceScheduleRequest.toEntity()` 제거, Service에서 팩토리 메서드 사용
*   **CLAUDE.md 업데이트:**
    *   필수 참조 문서 섹션 추가 (`DEVELOPMENT_CONVENTION.md` 링크)
*   **DEVELOPMENT_CONVENTION.md 업데이트:**
    *   Flyway 관련 내용 삭제
    *   신규 섹션 추가: 풍부한 도메인 모델, DTO 설계/Validation, 예외 처리, 엔티티 설계 규칙, 트랜잭션 관리, 문서 관리

## 2025년 12월 17일 (수)

*   **공연/좌석 등록 플로우 문서 업데이트:**
    *   `performance-todo.md`의 최신 설계 변경 사항을 반영하여 `공연_좌석_등록_플로우.md` 문서를 업데이트했습니다.
    *   주요 변경 사항: 공연장(`Venue`)에 대표번호(`phoneNumber`) 필드 추가, 등급별 허용좌석수(`VenueSeatCapacity`) 등록 절차 추가, 기획사(`Company`) 등록 및 공연(`Performance`)과의 연관 관계 추가, 공연 회차(`PerformanceSchedule`)에 티켓 옵션(`TicketOption`) 직접 연결, 좌석 초기화 로직 설명 개선.
    *   FE 협의 필요 사항 및 데이터 관계도 업데이트, 기존 불필요한 협의 내용은 제거했습니다.
    *   DDD 리팩토링(향후 과제) 섹션을 추가하여 코드 품질 개선 방향을 명시했습니다.
*   **Swagger API 문서 권한 정보 추가:**
    *   `AdminAuthController.kt` 및 `AuthController.kt` 파일의 각 API 엔드포인트 `@Operation` description에 권한 정보 (`**권한: 누구나**`, `**권한: USER, ADMIN**` 등)를 추가하여 API 문서의 명확성을 높였습니다.
*   **성능 TODO 문서 업데이트:**
    *   `공연_좌석_등록_플로우.md` 문서 업데이트로 인해 완료된 항목들을 `contexts/performance-todo.md`에 반영하여 진행 상황을 업데이트했습니다.

## 2025년 12월 16일 (화)

*   **좌석 시스템 재설계 준비:**
    *   FE 피드백을 반영하여 좌석 등록 방식을 JSON 기반으로 변경하기 위해, 기존의 좌석 및 예매 관련 코드(엔티티, 컨트롤러, 서비스 등)를 모두 삭제했습니다.
    *   좌석 시스템 재설계 분석 문서를 추가했습니다. (`documents/design/신규_좌석_시스템_구현_계획.md`)
*   **로깅 설정 개선:**
    *   `NoResourceFoundException`으로 인한 불필요한 WARN 로그가 발생하지 않도록 `logback-spring.xml`에 `PageNotFound` 로거 레벨을 조정했습니다.
*   **인증 API 개선:**
    *   `GET /api/auth/me` 엔드포인트 구현: 로그인된 사용자 정보를 조회합니다.
    *   `/api/auth/me` 엔드포인트에 `@PreAuthorize("isAuthenticated()")` 적용 및 `SecurityConfig` 업데이트를 통해 인증된 사용자만 접근 가능하도록 보안 강화.
*   **로그인 API 개선:**
    *   로그인 성공 시 사용자 정보(`UserResponse`)를 응답 본문에 포함하도록 변경.
    *   `LoginResponse` DTO 추가 (token + user).
*   **쿠키 SameSite 정책 수정:**
    *   로컬 환경에서 `SameSite=None` + `Secure=false` 조합이 브라우저에서 무시되는 문제 해결.
    *   로컬: `SameSite=Lax`, 프로덕션: 기존 정책 유지.
*   **쿠키 도메인 판단 로직 개선:**
    *   프론트가 localhost에서 개발서버(api.ticket.devhong.cc) 호출 시 쿠키 도메인이 `.devhong.cc`로 정상 설정되도록 수정.
    *   Host가 프로덕션 도메인을 포함하면 Origin과 무관하게 프로덕션으로 판단.
*   **JWT 인증 개선:**
    *   `JwtAuthenticationFilter`에 `Authorization: Bearer {token}` 헤더 지원 추가 (Swagger, Postman 등).
    *   `JwtTokenProvider.getAuthentication()`에서 `UserDetails` 객체 생성하도록 수정 (`@AuthenticationPrincipal` 정상 동작).
*   **SecurityConfig 업데이트:**
    *   `/.well-known/**` 경로 permitAll 추가 (Chrome DevTools 요청).
    *   `/actuator/prometheus` 경로 permitAll 추가 (모니터링용).
*   **Grafana 모니터링 스택 구성:**
    *   `micrometer-registry-prometheus` 의존성 추가.
    *   Actuator에 `prometheus`, `metrics` 엔드포인트 노출.
    *   로컬 테스트용 `monitoring/` Docker Compose 설정 추가 (Grafana, Prometheus, Loki, Promtail).
*   **요구사항 문서 정리:**
    *   `contexts/performance.md` 요구사항 문서 업데이트 (출연진, 회차 등 추가).
    *   `contexts/performance-todo.md` TODO 문서 통합 및 우선순위 정리.
    *   `contexts/TODO_performance_and_reservation.md` 삭제 (통합 완료).


## 2025년 12월 15일 (월)

*   **이메일 인증 OTP 재요청 기능 수정:**
    *   동일 이메일로 OTP 재요청 시 unique 제약 충돌 방지 (`deleteByEmail` + `flush`)
*   **JPA 설정 변경:**
    *   `ddl-auto: create` → `update`로 변경하여 재기동 시 데이터 유실 방지
*   **로깅 설정 개선:**
    *   현재 로그를 항상 `smarter-store.log`에 기록하도록 변경.
    *   롤오버된 파일만 날짜+인덱스 형식(`smarter-store-2025-12-15.0.log`)으로 아카이브.
*   **DDD Rich Domain Model 리팩토링:**
    *   Service에 있던 비즈니스 로직을 Domain으로 이동하여 DDD 원칙에 맞게 리팩토링했습니다.
    *   **Auth 도메인:**
        *   `User.create()` 팩토리 메서드 추가 (비밀번호 인코딩 포함)
        *   `User.verifyEmail()`, `changePassword()`, `updateProfile()` 도메인 메서드 추가
        *   `User.issueRefreshToken()`, `rotateRefreshToken()` - Aggregate Root 패턴 적용
        *   `EmailVerificationToken.create()`, `validate()` 도메인 메서드 추가
        *   `RefreshToken.create()`, `validateNotExpired()` 도메인 메서드 추가
    *   **Admin 도메인:**
        *   `Admin.create()` 팩토리 메서드 추가 (비밀번호 인코딩 포함)
        *   `Admin.validatePassword()`, `changePassword()`, `updateProfile()` 도메인 메서드 추가
        *   `AdminSignupRequest.toEntity()` 제거
    *   **Reservation 도메인:**
        *   `Reservation.createWithStock()` 팩토리 메서드 추가 (예매번호 생성, 가격 계산 포함)
        *   `Reservation.matchesPhone()` 연락처 검증 메서드 추가
        *   `ScheduleTicketStock.canReserve()`, `validateAndDecreaseStock()`, `calculateTotalPrice()` 추가
    *   DDD 리팩토링 태스크 목록 문서 생성 (`contexts/ddd-refactoring-tasks.md`)
*   **회원가입 인증 흐름 리팩토링 및 오류 해결:**
    *   **DB 무결성 오류 해결**: DDD 리팩토링 과정에서 발생한 `DataIntegrityViolationException`을 해결했습니다. `EmailVerificationToken`과 `User`의 관계를 분리하고, 로컬 환경에서는 `ddl-auto: create`를 사용하도록 변경하여 문제를 해결했습니다.
    *   **2단계 OTP 인증 흐름 도입**: 사용자 요구사항에 맞춰, 6자리 영숫자 OTP를 사용하는 2단계 인증 프로세스를 구현했습니다.
        *   `EmailVerificationToken`이 OTP 생성 및 '확인됨' 상태(`isConfirmed`)를 관리하도록 수정했습니다.
        *   OTP를 검증하는 별도 API (`/api/auth/confirm-otp`)를 추가했습니다.
        *   `signup` API는 사전에 OTP 인증이 완료된 이메일만 가입 처리하도록 변경했습니다.
*   **이메일 템플릿 유지보수성 개선:**
    *   `spring-boot-starter-thymeleaf` 의존성을 추가했습니다.
    *   `EmailService`에 하드코딩 되어있던 HTML 본문을 별도의 Thymeleaf 템플릿 파일(`src/main/resources/templates/mail/verification.html`)로 분리하여 유지보수성을 향상시켰습니다.
*   **메일 발송 오류 진단 및 해결:**
    *   개발 중 발생했던 `PKIX path building failed` (SSL 인증서 신뢰) 오류와 `SocketTimeoutException` (응답 시간 초과) 오류의 원인이 코드가 아닌 네트워크 환경(이더넷 프록시) 문제임을 진단했습니다.
    *   이에 따라 불필요하게 추가되었던 메일 관련 SSL 및 타임아웃 설정을 모두 제거하고 원래의 안정적인 상태로 복원했습니다.

## 2025년 12월 11일 (목)

*   **Flyway → JPA DDL-auto 전환:**
    *   로컬 개발 환경에서 Flyway 마이그레이션 대신 JPA `ddl-auto: update` 방식으로 전환.
    *   `Flyway_JPA_전략.md` 문서에 baseline 리셋 및 히스토리 관리 가이드 추가.

*   **쿠키 localhost 판단 로직 개선:**
    *   `CookieService`의 `isLocalhost()` 함수가 Origin 헤더만으로 판단하던 것을 Host 헤더로 fallback하도록 수정.
    *   Swagger UI 등 Origin 없이 요청하는 경우에도 로컬 환경으로 인식하여 `Secure=false` 쿠키 설정.
    *   `AuthController`, `AdminAuthController`에 Host 헤더 파라미터 추가.

*   **공연/좌석 등록 플로우 가이드 업데이트:**
    *   FE 협의내용 검토 섹션 추가 (CreateSeatRequest 필드 상세, 등록 순서, 협의 필요 사항).

## 2025년 12월 10일 (수)

*   **쿠키 처리 로직 리팩토링:**
    *   `CookieService`를 도입하여 모든 인증 관련 쿠키 생성, 삭제 로직을 중앙 집중화.
    *   `AuthController` 및 `AdminAuthController`에서 `CookieService`를 사용하여 쿠키 처리를 간소화.
    *   쿠키 도메인 설정(`app.cookie.domain`)을 `application.yml`을 통해 관리하도록 변경하고, 로컬 환경에서는 도메인을 설정하지 않도록 조건부 처리.
    *   관련 문서(`JWT_쿠키_전략_가이드.md`) 업데이트.

*   **CORS 설정 환경변수화:**
    *   `application-local.yml`, `application-prod.yml`의 `cors.allowed-origins`를 `${CORS_ALLOWED_ORIGINS}` 환경변수로 변경.
    *   `.env` 파일에 `CORS_ALLOWED_ORIGINS` 항목 추가.

*   **Cross-site 쿠키 문제 해결 (localhost 개발 환경 지원):**
    *   `AuthController`, `AdminAuthController`에서 요청의 Origin 헤더를 확인하여 동적으로 쿠키 속성 설정.
    *   localhost/127.0.0.1에서 요청 시: `SameSite=None`, `Secure=false` 적용.
    *   그 외 도메인에서 요청 시: `SameSite=Lax/Strict`, `Secure=true` 유지.
    *   `cookie.secure` yml 설정 제거 (Origin 기반 동적 처리로 대체).
    *   프론트엔드 개발자가 localhost에서 개발서버 API에 직접 연결하여 쿠키 기반 인증 사용 가능.

*   **메일 설정 공통화:**
    *   `application.yml`로 메일 설정 이동 (host, port, smtp 옵션은 고정값).
    *   `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`만 환경변수로 관리.
    *   `application-local.yml`에서 중복 메일 설정 제거.

## 2025년 12월 9일 (화)

*   **대규모 기능 추가 및 리팩토링:**
    *   **좌석 예매 시스템**: 공연의 특정 좌석을 지정하여 예매하는 기능을 구현했습니다.
        *   관련 도메인: `Reservation`, `ScheduleSeat`, `Seat` 등 추가.
        *   예매 및 좌석 상태 관리를 위한 서비스, 컨트롤러, DTO, Repository 구현.
    *   **이메일 인증**: 회원가입 시 이메일 인증을 통해 계정을 활성화하는 기능을 추가했습니다.
        *   `EmailVerificationToken` 도메인 및 관련 서비스, DTO, Repository 구현.
    *   **SSE (Server-Sent Events)**: 좌석 상태의 실시간 업데이트를 위해 SSE를 도입했습니다. (관련 문서만 추가되었고, 코드는 아직 없는 것으로 보임)
    *   **리팩토링**:
        *   공통으로 사용되는 예외 처리, 엔티티 등을 `common` 패키지로 분리하여 코드 구조를 개선했습니다. (`BaseEntity`, `CustomException`, `ErrorResponse`, `GlobalExceptionHandler`)
        *   `ErrorCode`를 체계적으로 관리하도록 변경했습니다.
        *   `AsyncConfig`, `MailConfig` 등 새로운 설정 파일을 추가했습니다.
    *   **데이터베이스**: `Flyway`를 사용하여 좌석(`V9`), 예매(`V8`, `V10`, `V11`, `V12`), 이메일 인증(`V13`) 관련 테이블 스키마를 추가하고 변경했습니다. 또한, `V7`로 성능 가시성 플래그를 추가했습니다.
    *   **문서**: 신규 기능과 아키텍처 결정을 위한 설계 가이드 문서를 추가했습니다. (`documents/design/SSE_실시간_통신_가이드.md`, `documents/features/공연_이미지_업로드_가이드.md`, `documents/features/이메일_인증_프로세스_가이드.md`, `documents/features/좌석_예매_시스템_가이드.md`, `documents/guidelines/토큰_효율적_사용_가이드.md`)
    *   `application-local.yml` 및 `application-prod.yml`에 프론트엔드 URL 및 메일 설정 관련 플레이스홀더 추가.

## 2025년 12월 8일 (월)

*   **주요 기능 추가 및 리팩토링:**
    *   공연(Performance) 관련 기능 (Venue, Performance, PerformanceSchedule, TicketOption) 추가 및 관련 API 구현.
    *   초기 샘플 `Product` 관련 코드 및 데이터베이스 마이그레이션 삭제.
    *   프로젝트 구조를 계층형에서 기능/도메인 단위로 리팩토링 (Auth, Admin, Venue, Performance 도메인 모듈 및 공유 컴포넌트 분리).
    *   문서 디렉토리 구조 재편성 (`documents/setup`, `documents/infra`, `documents/design`, `documents/features`).
    *   JWT 쿠키 기반 인증 구현 및 관련 코드 개선.
    *   관련 문서(`공연_기능_가이드.md`, `아키텍처_DDD_가이드.md`, `초기_설정_가이드.md` 등) 및 `CHANGELOG.md` 업데이트.
    *   `SecurityConfig` 및 `ErrorCode` 등 공통 설정 업데이트.
    *   Swagger (`@Schema(description = "...")`) 및 DB (`COMMENT ON TABLE/COLUMN`)에 한글 설명 추가.


## 2025년 12월 7일 (일)

*   **Swagger Tag name 영문화:**
    *   API 그룹명을 영문으로 변경 (Orval 클라이언트 코드 생성 시 일관성 확보).
*   **README.md 간소화:**
    *   중복 내용 제거 및 상세 가이드는 `documents/` 폴더로 위임.
    *   프로젝트 개요, 기술 스택, 빌드/실행 명령어 중심으로 정리.
*   **CLAUDE.md 파일 생성:**
    *   Claude Code용 프로젝트 가이드 문서 추가.
*   **Swagger API 문서에 권한 정보 추가:**
    *   `OpenApiConfig.kt`에서 전역 `SecurityRequirement` 제거하여 API별 개별 설정 방식으로 변경.
    *   `AuthController`, `AdminAuthController`에 `@SecurityRequirements` 어노테이션 추가 (인증 불필요 API 표시).
    *   `ProductController` 각 API에 `@SecurityRequirement(name = "bearerAuth")` 추가 및 description에 권한 정보(ADMIN, USER) 명시.
*   **documents 폴더 문서 파일명 한글화:**
    *   12개 문서 파일명을 영문에서 한글로 변경 (예: `INITIAL_SETUP.md` → `초기_설정_가이드.md`).
    *   `README.md`, `CHANGELOG.md`의 문서 링크를 새 파일명에 맞게 업데이트.


## 2025년 12월 6일 (토)

*   **DB 접속정보 보안 강화:**
    *   `application-local.yml`의 하드코딩된 DB 접속정보(url, username, password)를 환경변수 참조 방식으로 변경.
    *   프로젝트 루트에 `.env` 파일 생성하여 민감한 정보 분리.
    *   `.gitignore`에 `.env`, `.env.local`, `.env.*` 패턴 추가하여 Git 추적 제외.
    *   `spring-dotenv` 라이브러리(4.0.0) 추가하여 `.env` 파일 자동 로드 지원.


## 2025년 12월 5일 (금)

*   **JWT WeakKeyException 해결 및 보안 강화:**
    *   `JwtTokenProvider.kt`에서 JWT Secret을 Base64 디코딩하여 사용하도록 수정.
    *   보안 강화를 위해 `application.yml`의 `jwt.secret`을 Base64 인코딩된 강력한 임의 키로 설정하도록 가이드 제공.
*   **IntelliJ IDEA 환경 변수 설정 가이드 제공:**
    *   IDE에서 환경 변수를 설정하는 방법 및 OS 환경 변수와의 우선순위 설명.
*   **Docker 배포 환경 설정 (local):**
    *   로컬 Docker 배포 가이드 문서(`documents/Docker_로컬_배포_가이드.md`) 작성.
    *   `docker run -d` 옵션 설명.


## 2025년 12월 4일 (목)

*   **프로젝트 초기화 및 Git-flow 설정:**
    *   `git-flow` 브랜치 모델 초기화 및 `develop` 브랜치 설정.
    *   `.gitignore` 설정 개선 (로그 파일 무시 처리).
    *   원격 GitHub 저장소 (`https://github.com/copy-in-action/smarter-store-api.git`) 연결 및 `main`, `develop` 브랜치 푸시 완료.
*   **애플리케이션 구동 및 빌드 환경 안정화:**
    *   `localhost:8080` 포트 사용 충돌 및 데이터베이스(`smarter_store` 등) 미존재 오류 진단 및 해결 가이드 제공.
    *   애플리케이션 구동 및 Swagger UI (`http://localhost:8080/swagger-ui.html`) 접근성 확인.
    *   Gradle 버전(9.2.1 -> 8.8) 다운그레이드 및 `bootJar` 빌드 호환성 문제 해결.
*   **로깅 설정 외부화:**
    *   `src/main/resources/application.yml`에 외부 로그 경로 (`/home/cic/logs/smarter-store/`) 설정 추가.
*   **Spring Profiles를 이용한 환경별 설정 분리:**
    *   `application.yml`을 공통 설정 및 기본 프로파일 활성화(`local`)용으로 리팩토링.
    *   `application-prod.yml` 파일 생성 (운영 DB, 로깅 경로, JWT secret 설정 포함).
    *   `application-local.yml` 파일 생성 (로컬 개발 DB, 로깅 경로, JWT secret 설정 포함).
*   **GitHub Actions Docker 배포 가이드 문서 작성:**
    *   `documents/GitHub_Actions_Docker_배포_가이드.md` 파일 생성 및 Docker 기반 배포 관련 상세 가이드 작성.


## 2025년 12월 3일 (수)

*   **프로젝트 초기 생성 및 기본 구조 확립:**
    *   Kotlin, Spring Boot, Gradle 기반의 `Smarter Store API` 프로젝트 초기 생성.
    *   기본적인 REST API 구조(Controller, Service, Domain, Repository) 설계 및 구현.
    *   PostgreSQL 데이터베이스 및 Flyway 마이그레이션 도구 설정.
    *   Spring Security, JWT를 이용한 보안 기본 설정.
    *   Springdoc OpenAPI(Swagger UI)를 통한 API 문서화 설정.
    *   Logback을 사용한 로깅 정책 설정 및 적용.
    *   테스트(JUnit 5, MockK) 환경 구성.