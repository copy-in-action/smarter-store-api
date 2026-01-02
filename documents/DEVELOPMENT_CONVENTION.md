# Smarter Store API 개발 컨벤션

## 개정이력

| 버전 | 일자 | 작성자 | 내용 |
|------|------|--------|------|
| 1.0 | 2025-12-29 | Gemini | 초기 컨벤션 수립 및 문서화 |
| 1.1 | 2026-01-02 | Claude | 비즈니스 로직 위치 판단 기준 추가 (1.1절) |

---

이 문서는 프로젝트의 핵심 코딩 규칙과 아키텍처 원칙을 정의합니다.

## 0. AI 에이전트 가이드 (Persona)
*   **페르소나**: 너는 시니어 백엔드 개발자이자 나의 든든한 파트너야.
*   **답변 원칙**: 모든 기술적인 답변은 한국어로 상세히 설명하고, 코드 리뷰는 꼼꼼하게 진행한다.
*   **목표**: 사용자(주니어/시니어 개발자)와 협력하여 유지보수성이 높고 안정적인 시스템을 구축한다.

## 1. 핵심 아키텍처 및 도메인 원칙
*   **DDD & Clean Architecture**: 비즈니스 로직은 도메인 엔티티에, 서비스는 도메인 간 조율에 집중. 의존성은 단방향 유지.
*   **Rich Domain Model**: 엔티티는 상태 변경 및 유효성 검증 로직을 포함함. Service에서 직접 Setter 호출 금지.
*   **Domain Factory**: 엔티티 생성은 DTO의 `toEntity()` 대신 엔티티 내부의 `create()` 팩토리 메서드 사용.

### 1.1 비즈니스 로직 위치 판단 기준

> **핵심 원칙**: "이 로직이 외부 의존성 없이 동작하는가?"

#### 판단 기준표

| 질문 | Yes → Domain | No → Service |
|------|--------------|--------------|
| 엔티티 자신의 상태만으로 판단 가능한가? | ✅ | |
| 외부 Repository 조회가 필요한가? | | ✅ |
| 다른 Aggregate를 수정해야 하는가? | | ✅ |
| 외부 시스템(API, 메시지큐) 호출이 필요한가? | | ✅ |
| 트랜잭션 경계 조율이 필요한가? | | ✅ |

#### Domain에 위치해야 하는 로직

```kotlin
// 1. 자기 상태 변경 + 유효성 검증
class Booking {
    fun confirm() {
        require(status == BookingStatus.PENDING) { "PENDING 상태만 확정 가능" }
        this.status = BookingStatus.CONFIRMED
    }
}

// 2. 자기 데이터 기반 계산
class Booking {
    fun calculateTotalPrice(): Long = seats.sumOf { it.price }
}

// 3. 비즈니스 규칙 캡슐화
class Booking {
    companion object {
        const val MAX_SEAT_COUNT = 4
    }

    fun addSeats(seats: List<SeatDetail>) {
        require(seats.size <= MAX_SEAT_COUNT) { "최대 ${MAX_SEAT_COUNT}석까지 선택 가능" }
        this.seats.addAll(seats)
    }
}

// 4. 값 객체(Value Object) 내 순수 계산 로직
data class SeatSelection(val seats: Set<SeatPosition>) {
    fun calculateChanges(newSelection: SeatSelection): SeatChangeResult {
        val kept = seats.intersect(newSelection.seats)
        val released = seats - newSelection.seats
        val added = newSelection.seats - seats
        return SeatChangeResult(kept, released, added)
    }
}
```

#### Service에 위치해야 하는 로직

```kotlin
// 1. 여러 Repository 조회/조합
fun startBooking(scheduleId: Long, userId: Long): Booking {
    val schedule = scheduleRepository.findById(scheduleId)  // 외부 조회
    val user = userRepository.findById(userId)              // 외부 조회
    return Booking.create(schedule, user)
}

// 2. 여러 Aggregate 수정 (트랜잭션 조율)
@Transactional
fun confirmBooking(bookingId: UUID) {
    val booking = bookingRepository.findById(bookingId)
    booking.confirm()                           // Booking Aggregate
    seatStatusRepository.updateToReserved(...)  // SeatStatus Aggregate
}

// 3. 외부 시스템 연동
fun notifyBookingConfirmed(booking: Booking) {
    slackService.send(...)      // 외부 API
    emailService.send(...)      // 외부 API
}

// 4. 도메인 간 조율 (Orchestration)
fun createSchedule(performanceId: Long, request: CreateScheduleRequest) {
    val performance = performanceRepository.findById(performanceId)
    val venue = venueRepository.findById(performance.venueId)
    val schedule = PerformanceSchedule.create(performance, venue, request.showTime)
    scheduleRepository.save(schedule)
}
```

#### 판단 플로우차트

```
로직 분석 시작
    │
    ▼
┌─────────────────────────────┐
│ Repository 조회가 필요한가? │
└─────────────────────────────┘
    │ Yes          │ No
    ▼              ▼
  Service    ┌─────────────────────────────┐
             │ 다른 Aggregate 수정이 필요? │
             └─────────────────────────────┘
                 │ Yes          │ No
                 ▼              ▼
              Service    ┌─────────────────────────┐
                         │ 외부 API 호출이 필요?   │
                         └─────────────────────────┘
                             │ Yes          │ No
                             ▼              ▼
                          Service       **Domain**
```

## 2. 코드 스타일 & 네이밍
*   **Kotlin Idiomatic**: Kotlin의 기능(null safety, extensions 등) 적극 활용.
*   **Naming**: 클래스/인터페이스(`PascalCase`), 변수/함수/프로퍼티(`camelCase`), 상수(`SCREAMING_SNAKE_CASE`).
*   **Formatting**: 들여쓰기 4칸 공백. 주석은 구현 배경(Why) 위주로 작성.

## 3. 계층 및 패키지 구조
*   **구조**: `com.github.copyinaction.[도메인].[layer]`
*   **Layer**: `controller`, `service`, `repository`, `domain`, `dto`

## 4. DTO 및 Validation
*   **Response DTO**: `companion object`의 `from(entity)` 메서드로 변환 로직 구현.
*   **File**: 도메인별 `[도메인]Dto.kt` 파일에 관련 DTO들을 모아서 정의.

### 4.1 검증(Validation) 원칙
검증 로직의 중복을 방지하고 유지보수성을 높이기 위해 다음과 같이 역할을 분담한다.

| 계층 | 역할 | 담당 범위 | 구현 방식 |
| :--- | :--- | :--- | :--- |
| **DTO** | **형식(Format) 검증** | - 필수값(`@NotNull`, `@NotBlank`)<br>- 데이터 타입 및 길이(`@Size`)<br>- 단순 포맷(`@Email`, `@Pattern`) | Spring Bean Validation (`@Valid`) |
| **Domain** | **업무 규칙(Business) 검증** | - 필드 간 관계 (예: `start < end`)<br>- 상태 기반 검증 (예: `PENDING` 상태만 취소 가능)<br>- 비즈니스 제약 조건 | 엔티티 내부 `validate()` 메서드<br>(`CustomException` 사용) |

> **Note**: DTO에서 검증 가능한 단순 포맷(이메일, 전화번호 등)은 도메인 엔티티에서 중복으로 검증하지 않는다. 도메인은 데이터가 **논리적으로 올바른지** 판단하는 데 집중한다.

## 5. API 설계 및 예외 처리
*   **RESTful API**: 리소스 중심 설계 및 적절한 HTTP 메서드 활용.
*   **Springdoc**: `@Tag`(케밥-케이스), `@Operation`, `@Schema` 활용.
*   **Exception**: `ErrorCode` enum 정의 및 `CustomException` 사용.
*   **Error Message**: 에러 발생 시 클라이언트가 원인을 파악할 수 있도록 `ErrorCode`의 메시지 또는 구체적인 예외 메시지를 응답 본문에 반드시 포함한다.
*   **Status Mapping**: 404(NotFound), 400(BadRequest), 409(Conflict), 401/403(Security).

## 6. 데이터베이스 및 엔티티
*   **Entity**: `BaseEntity` 상속(`createdAt`, `updatedAt`), `@Comment` 필수, ID 초기값 `0`.
*   **Reserved Words**: DB 예약어 필드명/컬럼명 사용 금지 (예: `status` -> `seatStatus`, `order` -> `bookingOrder`, `user` -> `siteUser` 등).
*   **Transaction**: 클래스 레벨 `@Transactional(readOnly = true)`, 쓰기 메서드에만 `@Transactional` 명시.
*   **DDL-auto**: `update` 기반 자동 스키마 반영.

## 7. 테스팅 및 환경 설정
*   **Test**: JUnit 5 + MockK. 단위 테스트 및 통합 테스트(`@SpringBootTest`) 병행.
*   **Profiles**: `local`, `prod` 분리 및 환경 변수(`${VAR}`) 주입.
*   **Secret**: `.env` 파일을 통한 로컬 환경 변수 관리.

## 8. Git 전략 및 문서화
*   **Branch**: `main`(배포용), `develop`(개발용) 2브랜치 전략.
*   **Commit**: Conventional Commits (`feat`, `fix`, `refactor`, `docs`, `chore`).
*   **Changelog**: 상세 변경 내역은 `CHANGELOG.md`에 일자별로 기록.
*   **Docs**: `documents/` 하위 폴더별(`01_개발_컨벤션`, `02_시스템_아키텍처` 등) 관리.
*   **Revision History**: 모든 문서 작성 시 상단에 개정이력 테이블(버전, 날짜, 작성자, 내용)을 포함한다.
