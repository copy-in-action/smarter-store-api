# Audit 로그 설계

## 개정이력

| 버전 | 일자 | 작성자 | 내용 |
|------|------|--------|------|
| 0.1 | 2025-12-29 | Claude | 초안 작성 |
| 1.0 | 2025-12-29 | Claude | Phase 1~2 구현 완료 - 기본 구조 및 예매/인증 API 적용 |
| 1.1 | 2025-12-29 | Claude | Phase 2~3 구현 완료 - 관리자 API @Auditable 적용, 조회/통계 API 구현 |

---

## 1. 개요

### 1.1 목적
- **보안/규정 준수**: 로그인, 결제, 예매 등 핵심 액션 기록
- **운영 모니터링**: 관리자 액션, 좌석 점유 등 운영 상태 추적

### 1.2 설계 원칙
- FE 추가 API 호출 없이 **백엔드 AOP로 자동 기록**
- 인증된 사용자(로그인 사용자)의 액션만 기록
- 비동기 처리로 API 응답 성능에 영향 없음

---

## 2. 아키텍처

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Controller │ ──▶ │  @Auditable │ ──▶ │  AuditAspect│
│  (API)      │     │  Annotation │     │  (AOP)      │
└─────────────┘     └─────────────┘     └──────┬──────┘
                                               │
                                               ▼ (Async)
                                        ┌─────────────┐
                                        │ AuditLog    │
                                        │ Repository  │
                                        └─────────────┘
```

### 2.1 컴포넌트

| 컴포넌트 | 역할 |
|----------|------|
| `@Auditable` | 기록 대상 메서드 마킹 어노테이션 |
| `AuditAspect` | AOP로 어노테이션 감지 및 로그 생성 |
| `AuditLogService` | 비동기로 로그 저장 |
| `AuditLogRepository` | DB 저장 |

---

## 3. 데이터 모델

### 3.1 AuditLog 엔티티

```kotlin
@Entity
@Table(
    name = "audit_log",
    indexes = [
        Index(name = "idx_audit_log_user", columnList = "user_id"),
        Index(name = "idx_audit_log_action", columnList = "action"),
        Index(name = "idx_audit_log_created", columnList = "created_at"),
        Index(name = "idx_audit_log_target", columnList = "target_type, target_id")
    ]
)
class AuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "user_email", length = 100)
    val userEmail: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", length = 20)
    val userRole: Role?,

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    val action: AuditAction,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 30)
    val targetType: AuditTargetType?,

    @Column(name = "target_id", length = 100)
    val targetId: String?,

    @Column(name = "description", length = 500)
    val description: String?,

    @Column(name = "request_path", length = 200)
    val requestPath: String?,

    @Column(name = "request_method", length = 10)
    val requestMethod: String?,

    @Column(name = "request_body", columnDefinition = "TEXT")
    val requestBody: String?,

    @Column(name = "response_status")
    val responseStatus: Int?,

    @Column(name = "ip_address", length = 50)
    val ipAddress: String?,

    @Column(name = "user_agent", length = 500)
    val userAgent: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

### 3.2 AuditAction Enum

```kotlin
enum class AuditAction(val category: AuditCategory, val description: String) {
    // 인증
    LOGIN(AuditCategory.AUTH, "로그인"),
    LOGIN_FAILED(AuditCategory.AUTH, "로그인 실패"),
    LOGOUT(AuditCategory.AUTH, "로그아웃"),

    // 계정
    SIGNUP(AuditCategory.ACCOUNT, "회원가입"),
    PASSWORD_CHANGE(AuditCategory.ACCOUNT, "비밀번호 변경"),
    PROFILE_UPDATE(AuditCategory.ACCOUNT, "프로필 수정"),
    ACCOUNT_DELETE(AuditCategory.ACCOUNT, "회원탈퇴"),

    // 예매
    BOOKING_START(AuditCategory.BOOKING, "예매 시작"),
    BOOKING_CONFIRM(AuditCategory.BOOKING, "예매 확정"),
    BOOKING_CANCEL(AuditCategory.BOOKING, "예매 취소"),

    // 결제
    PAYMENT_REQUEST(AuditCategory.PAYMENT, "결제 요청"),
    PAYMENT_COMPLETE(AuditCategory.PAYMENT, "결제 완료"),
    PAYMENT_FAIL(AuditCategory.PAYMENT, "결제 실패"),
    REFUND_REQUEST(AuditCategory.PAYMENT, "환불 요청"),
    REFUND_COMPLETE(AuditCategory.PAYMENT, "환불 완료"),

    // 관리자 - 공연
    PERFORMANCE_CREATE(AuditCategory.ADMIN, "공연 등록"),
    PERFORMANCE_UPDATE(AuditCategory.ADMIN, "공연 수정"),
    PERFORMANCE_DELETE(AuditCategory.ADMIN, "공연 삭제"),

    // 관리자 - 회차
    SCHEDULE_CREATE(AuditCategory.ADMIN, "회차 등록"),
    SCHEDULE_UPDATE(AuditCategory.ADMIN, "회차 수정"),
    SCHEDULE_DELETE(AuditCategory.ADMIN, "회차 삭제"),

    // 관리자 - 공연장
    VENUE_CREATE(AuditCategory.ADMIN, "공연장 등록"),
    VENUE_UPDATE(AuditCategory.ADMIN, "공연장 수정"),

    // 좌석
    SEAT_HOLD(AuditCategory.SEAT, "좌석 점유"),
    SEAT_RELEASE(AuditCategory.SEAT, "좌석 해제")
}

enum class AuditCategory {
    AUTH,       // 인증
    ACCOUNT,    // 계정
    BOOKING,    // 예매
    PAYMENT,    // 결제
    ADMIN,      // 관리자
    SEAT        // 좌석
}
```

### 3.3 AuditTargetType Enum

```kotlin
enum class AuditTargetType {
    USER,
    BOOKING,
    PAYMENT,
    PERFORMANCE,
    SCHEDULE,
    VENUE,
    SEAT
}
```

---

## 4. 구현 상세

### 4.1 @Auditable 어노테이션

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Auditable(
    val action: AuditAction,
    val targetType: AuditTargetType = AuditTargetType.USER,
    val targetIdParam: String = "",      // 파라미터명으로 targetId 추출
    val description: String = "",
    val includeRequestBody: Boolean = false
)
```

### 4.2 AuditAspect

```kotlin
@Aspect
@Component
class AuditAspect(
    private val auditLogService: AuditLogService,
    private val httpServletRequest: HttpServletRequest
) {
    @Around("@annotation(auditable)")
    fun audit(joinPoint: ProceedingJoinPoint, auditable: Auditable): Any? {
        val startTime = System.currentTimeMillis()
        var responseStatus = 200
        var exception: Exception? = null

        try {
            val result = joinPoint.proceed()
            return result
        } catch (e: Exception) {
            exception = e
            responseStatus = when (e) {
                is CustomException -> 400
                else -> 500
            }
            throw e
        } finally {
            saveAuditLog(joinPoint, auditable, responseStatus, exception)
        }
    }

    @Async
    private fun saveAuditLog(
        joinPoint: ProceedingJoinPoint,
        auditable: Auditable,
        responseStatus: Int,
        exception: Exception?
    ) {
        val userId = SecurityContextHolder.getContext() 
            .authentication?.let { (it.principal as? CustomUserDetails)?.id } 
            ?: return  // 비인증 사용자는 기록하지 않음

        val targetId = extractTargetId(joinPoint, auditable.targetIdParam)

        auditLogService.save(
            AuditLog(
                userId = userId,
                userEmail = getCurrentUserEmail(),
                userRole = getCurrentUserRole(),
                action = auditable.action,
                targetType = auditable.targetType,
                targetId = targetId,
                description = auditable.description.ifEmpty { auditable.action.description },
                requestPath = httpServletRequest.requestURI,
                requestMethod = httpServletRequest.method,
                requestBody = if (auditable.includeRequestBody) getRequestBody(joinPoint) else null,
                responseStatus = responseStatus,
                ipAddress = getClientIp(),
                userAgent = httpServletRequest.getHeader("User-Agent")
            )
        )
    }
}
```

### 4.3 AuditLogService

```kotlin
@Service
class AuditLogService(
    private val auditLogRepository: AuditLogRepository
) {
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun save(auditLog: AuditLog) {
        auditLogRepository.save(auditLog)
    }

    // 로그인 실패 등 인증 전 이벤트용
    fun saveAuthEvent(
        action: AuditAction,
        email: String?,
        ipAddress: String?,
        userAgent: String?,
        success: Boolean
    ) {
        auditLogRepository.save(
            AuditLog(
                userId = 0,  // 미인증
                userEmail = email,
                action = action,
                description = if (success) "성공" else "실패",
                ipAddress = ipAddress,
                userAgent = userAgent
            )
        )
    }
}
```

---

## 5. 적용 예시

### 5.1 예매 API

```kotlin
@RestController
@RequestMapping("/api/bookings")
class BookingController(
    private val bookingService: BookingService
) {
    @Auditable(
        action = AuditAction.BOOKING_START,
        targetType = AuditTargetType.BOOKING,
        includeRequestBody = true
    )
    @PostMapping("/start")
    fun startBooking(@RequestBody request: StartBookingRequest, ...) { }

    @Auditable(
        action = AuditAction.BOOKING_CONFIRM,
        targetType = AuditTargetType.BOOKING,
        targetIdParam = "bookingId"
    )
    @PostMapping("/{bookingId}/confirm")
    fun confirmBooking(@PathVariable bookingId: UUID, ...) { }

    @Auditable(
        action = AuditAction.BOOKING_CANCEL,
        targetType = AuditTargetType.BOOKING,
        targetIdParam = "bookingId"
    )
    @DeleteMapping("/{bookingId}")
    fun cancelBooking(@PathVariable bookingId: UUID, ...) { }
}
```

### 5.2 관리자 API

```kotlin
@RestController
@RequestMapping("/api/admin/performances")
class AdminPerformanceController(...) {

    @Auditable(
        action = AuditAction.PERFORMANCE_CREATE,
        targetType = AuditTargetType.PERFORMANCE,
        includeRequestBody = true
    )
    @PostMapping
    fun createPerformance(@RequestBody request: CreatePerformanceRequest) { }

    @Auditable(
        action = AuditAction.PERFORMANCE_UPDATE,
        targetType = AuditTargetType.PERFORMANCE,
        targetIdParam = "performanceId"
    )
    @PutMapping("/{performanceId}")
    fun updatePerformance(@PathVariable performanceId: Long, ...) { }
}
```

### 5.3 인증 API (특수 처리)

```kotlin
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val auditLogService: AuditLogService
) {
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest, httpRequest: HttpServletRequest): ResponseEntity<*> {
        return try {
            val response = authService.login(request)

            // 로그인 성공 기록
            auditLogService.saveAuthEvent(
                action = AuditAction.LOGIN,
                email = request.email,
                ipAddress = getClientIp(httpRequest),
                userAgent = httpRequest.getHeader("User-Agent"),
                success = true
            )

            ResponseEntity.ok(response)
        } catch (e: Exception) {
            // 로그인 실패 기록
            auditLogService.saveAuthEvent(
                action = AuditAction.LOGIN_FAILED,
                email = request.email,
                ipAddress = getClientIp(httpRequest),
                userAgent = httpRequest.getHeader("User-Agent"),
                success = false
            )
            throw e
        }
    }
}
```

---

## 6. 조회 API (관리자용)

### 6.1 API 목록

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/admin/audit-logs` | 감사 로그 목록 조회 |
| GET | `/api/admin/audit-logs/{id}` | 감사 로그 상세 조회 |
| GET | `/api/admin/audit-logs/users/{userId}` | 특정 사용자 로그 조회 |
| GET | `/api/admin/audit-logs/stats` | 감사 로그 통계 |

### 6.2 목록 조회

```
GET /api/admin/audit-logs
    ?userId=123
    &action=BOOKING_CONFIRM
    &category=BOOKING
    &targetType=BOOKING
    &targetId=uuid-xxx
    &from=2025-01-01T00:00:00
    &to=2025-01-31T23:59:59
    &page=0
    &size=20
    &sort=createdAt,desc
```

**Response**
```json
{
  "content": [
    {
      "id": 1,
      "userId": 123,
      "userEmail": "user@example.com",
      "userRole": "USER",
      "action": "BOOKING_CONFIRM",
      "actionDescription": "예매 확정",
      "category": "BOOKING",
      "targetType": "BOOKING",
      "targetId": "uuid-xxx",
      "description": "예매 확정",
      "requestPath": "/api/bookings/uuid-xxx/confirm",
      "requestMethod": "POST",
      "responseStatus": 200,
      "ipAddress": "192.168.1.1",
      "createdAt": "2025-12-29T14:30:00"
    }
  ],
  "pageable": { ... },
  "totalElements": 150,
  "totalPages": 8
}
```

### 6.3 통계

```
GET /api/admin/audit-logs/stats
    ?from=2025-01-01
    &to=2025-01-31
```

**Response**
```json
{
  "period": {
    "from": "2025-01-01",
    "to": "2025-01-31"
  },
  "totalCount": 15000,
  "byCategory": {
    "AUTH": 5000,
    "BOOKING": 4000,
    "PAYMENT": 2000,
    "ADMIN": 500,
    "SEAT": 3500
  },
  "byAction": [
    { "action": "LOGIN", "count": 4500 },
    { "action": "BOOKING_START", "count": 3000 },
    { "action": "BOOKING_CONFIRM", "count": 2500 }
  ],
  "topUsers": [
    { "userId": 123, "email": "user@example.com", "count": 150 }
  ]
}
```

---

## 7. 보안 및 성능

### 7.1 민감 정보 처리

```kotlin
// 요청 본문에서 민감 정보 마스킹
fun maskSensitiveData(requestBody: String): String {
    return requestBody
        .replace(Regex("\"password\"\\s*:\\s*\"[^\"]+\"" ), "\"password\":\"***\"")
        .replace(Regex("\"cardNumber\"\\s*:\\s*\"[^\"]+\"" ), "\"cardNumber\":\"***\"")
}
```

### 7.2 성능 고려사항

| 항목 | 전략 |
|------|------|
| 비동기 저장 | `@Async`로 API 응답에 영향 없음 |
| 배치 삭제 | 90일 이상 로그 배치로 아카이빙/삭제 |
| 인덱스 | userId, action, createdAt 복합 인덱스 |
| 파티셔닝 | 월별 파티셔닝 고려 (대용량 시) |

### 7.3 로그 보관 정책

| 카테고리 | 보관 기간 | 비고 |
|----------|----------|------|
| AUTH | 1년 | 보안 감사용 |
| PAYMENT | 5년 | 금융 규정 |
| BOOKING | 3년 | 서비스 기록 |
| ADMIN | 1년 | 운영 기록 |
| SEAT | 90일 | 운영 모니터링 |

---

## 8. 구현 순서

### Phase 1: 기본 구조
1. AuditLog 엔티티 생성
2. AuditAction, AuditTargetType Enum 정의
3. @Auditable 어노테이션 생성
4. AuditAspect 구현

### Phase 2: 적용
1. 인증 API 적용 (로그인/로그아웃)
2. 예매 API 적용
3. 관리자 API 적용

### Phase 3: 조회 API
1. 목록 조회 API
2. 통계 API
3. 관리자 UI 연동

### Phase 4: 운영
1. 보관 정책 배치 구현
2. 모니터링 대시보드 연동

---

## 9. 구현 현황

### 9.1 완료 (v1.1)

| 구분 | 파일 | 설명 |
|------|------|------|
| Enum | `audit/domain/AuditEnums.kt` | AuditAction, AuditCategory, AuditTargetType |
| Entity | `audit/domain/AuditLog.kt` | 감사 로그 엔티티 (인덱스 포함) |
| Repository | `audit/repository/AuditLogRepository.kt` | 필터 쿼리, 통계 쿼리 포함 |
| Annotation | `audit/annotation/Auditable.kt` | AOP 마킹 어노테이션 |
| Aspect | `audit/aspect/AuditAspect.kt` | @Around로 자동 감사 로그 기록 |
| Service | `audit/service/AuditLogService.kt` | 비동기 저장, 민감정보 마스킹, 조회/통계 |
| DTO | `audit/dto/AuditLogDto.kt` | Response, Stats DTO |
| Controller | `audit/controller/AdminAuditLogController.kt` | 관리자 조회/통계 API |
| Config | `SmarterStoreApiApplication.kt` | @EnableAsync 추가 |

### 9.2 적용된 API

| 컨트롤러 | 메서드 | AuditAction | 비고 |
|----------|--------|-------------|------|
| **예매** ||||
| BookingController | startBooking | BOOKING_START | requestBody 포함 |
| BookingController | confirmBooking | BOOKING_CONFIRM | bookingId 추출 |
| BookingController | cancelBooking | BOOKING_CANCEL | bookingId 추출 |
| **인증** ||||
| AuthController | logout | LOGOUT | @Auditable 사용 |
| AuthService | login | LOGIN / LOGIN_FAILED | 직접 호출 (인증 전) |
| AuthService | signup | SIGNUP | 직접 호출 (인증 전) |
| **관리자 - 공연** ||||
| AdminPerformanceController | createPerformance | PERFORMANCE_CREATE | requestBody 포함 |
| AdminPerformanceController | updatePerformance | PERFORMANCE_UPDATE | id 추출, requestBody 포함 |
| AdminPerformanceController | deletePerformance | PERFORMANCE_DELETE | id 추출 |
| **관리자 - 회차** ||||
| AdminPerformanceScheduleController | createSchedule | SCHEDULE_CREATE | performanceId 추출, requestBody 포함 |
| AdminPerformanceScheduleController | updateSchedule | SCHEDULE_UPDATE | scheduleId 추출, requestBody 포함 |
| AdminPerformanceScheduleController | deleteSchedule | SCHEDULE_DELETE | scheduleId 추출 |
| **관리자 - 공연장** ||||
| AdminVenueController | createVenue | VENUE_CREATE | requestBody 포함 |
| AdminVenueController | updateVenue | VENUE_UPDATE | id 추출, requestBody 포함 |
| AdminVenueController | deleteVenue | VENUE_DELETE | id 추출 |
| AdminVenueController | updateSeatingChart | VENUE_SEATING_CHART_UPDATE | id 추출, requestBody 포함 |
| **관리자 - 공지사항** ||||
| AdminNoticeController | createNotice | NOTICE_CREATE | requestBody 포함 |
| AdminNoticeController | updateNotice | NOTICE_UPDATE | id 추출, requestBody 포함 |
| AdminNoticeController | deleteNotice | NOTICE_DELETE | id 추출 |

### 9.3 관리자 조회 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/admin/audit-logs` | 감사 로그 목록 조회 (필터 지원) |
| GET | `/api/admin/audit-logs/{id}` | 감사 로그 상세 조회 |
| GET | `/api/admin/audit-logs/users/{userId}` | 특정 사용자 로그 조회 |
| GET | `/api/admin/audit-logs/stats` | 감사 로그 통계 |

### 9.4 미구현 (향후 작업)

- [ ] 결제 API @Auditable 적용
- [ ] 보관 정책 배치 (90일 이상 아카이빙)

---

## 10. 트러블슈팅

### 10.1 JPQL 및 PostgreSQL 호환성 문제
- **현상**: 감사 로그 조회 시 `ERROR: could not determine data type of parameter $11` 및 `Could not resolve attribute 'string'` 에러 발생
- **원인**: 
    - PostgreSQL은 JPQL의 `? IS NULL` 조건에서 파라미터 타입이 명확하지 않을 경우 에러를 발생시킴.
    - Hibernate 6.x에서 Enum 파라미터(`AuditAction` 등)가 NULL일 때 JPQL 파싱 과정에서 Enum 내부 메타데이터(`string` 속성) 참조 오류 발생.
- **해결**: 복잡한 `@Query` JPQL을 제거하고 **Spring Data JPA Specification(동적 쿼리)** 방식으로 전환하여 타입 안전성과 가독성 확보.

```kotlin
// AuditLogService.kt (수정 후)
val spec = Specification<AuditLog> { root, _, cb ->
    val predicates = mutableListOf<Predicate>()
    userId?.let { predicates.add(cb.equal(root.get<Long>("userId"), it)) }
    action?.let { predicates.add(cb.equal(root.get<AuditAction>("action"), it)) }
    // ...
    cb.and(*predicates.toTypedArray())
}
return auditLogRepository.findAll(spec, pageable)
```

### 10.2 Swagger Pageable 파라미터 오류
- **현상**: Swagger UI에서 `Pageable` 객체의 `sort` 파라미터 기본값이 `["string"]`으로 전송되어 서버에서 `PropertyReferenceException` 발생 (No property 'string' found for type 'AuditLog').
- **해결**: 컨트롤러의 `Pageable` 파라미터에 `@Parameter` 어노테이션을 추가하여 올바른 예시(`createdAt,desc`)를 제공하고 사용자 혼란 방지.

```kotlin
@Parameter(
    name = "pageable",
    description = "페이징 및 정렬 설정 (예: page=0&size=20&sort=createdAt,desc)",
    example = "{\"page\": 0, \"size\": 20, \"sort\": [\"createdAt,desc\"]}"
)
@PageableDefault(...) pageable: Pageable
```

### 10.3 Spring Data Web Support - JSON 직렬화 경고
- **현상**: 애플리케이션 실행 시 `For a stable JSON structure, please use Spring Data's PagedModel ...` 경고 로그 발생. Spring Data 3.x 업데이트로 인해 `Page` 인터페이스의 JSON 직렬화 구조가 변경될 수 있음을 안내.
- **해결**: `SmarterStoreApiApplication.kt`에 설정을 추가하여 안정적인 `PagedModel` 구조(DTO 방식)로 직렬화하도록 고정.

```kotlin
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@SpringBootApplication
class SmarterStoreApiApplication
```
