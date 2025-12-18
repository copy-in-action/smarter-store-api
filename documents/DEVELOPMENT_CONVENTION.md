# Smarter Store API 개발 컨벤션 가이드

이 문서는 `smarter-store-api` 프로젝트의 개발 컨벤션을 정의하여, 코드의 일관성과 품질을 유지하고 협업 효율성을 높이는 것을 목표로 합니다.

## 1. 일반 원칙

*   **도메인 주도 설계 (DDD)**: 비즈니스 도메인의 복잡성을 관리하기 위해 DDD 원칙을 따릅니다. 도메인 엔티티는 비즈니스 로직을 포함하며, 서비스 계층은 도메인 간의 조율을 담당합니다.
*   **테스트 주도 개발 (TDD)**: 새로운 기능을 개발하거나 버그를 수정할 때 테스트 코드를 먼저 작성하여 기능을 검증하고 리팩토링을 용이하게 합니다.
*   **클린 아키텍처 지향**: 계층 간의 관심사를 분리하고 의존성을 단방향으로 유지하여 유연하고 유지보수하기 쉬운 구조를 지향합니다.

## 2. 언어 및 플랫폼

*   **언어**: Kotlin
*   **프레임워크**: Spring Boot 3.3.0
*   **런타임**: Java 21 (Docker 환경에서는 Temurin 배포판 사용)

## 3. 빌드 시스템

*   **빌드 도구**: Gradle (Kotlin DSL 사용)
    *   `build.gradle.kts`: 의존성 관리, 플러그인 설정, Java 버전 등을 정의합니다.

## 4. 코드 스타일 및 포맷팅

*   **Kotlin Idiomatic**: Kotlin의 관용적인 표현과 기능을 적극 활용합니다.
*   **네이밍 컨벤션**:
    *   클래스, 인터페이스, 객체: `PascalCase`
    *   변수, 함수, 프로퍼티: `camelCase`
    *   상수: `SCREAMING_SNAKE_CASE`
    *   패키지: `lowercase.dot.separated`
*   **들여쓰기**: 4칸 공백
*   **가독성**: 코드 블록, 함수, 클래스 간의 적절한 빈 줄 사용
*   **주석**: 코드의 *무엇을* 설명하기보다는 *왜* 그렇게 구현되었는지, 복잡한 로직의 배경 설명을 위주로 작성합니다. Javadoc/Kdoc 스타일의 문서 주석을 활용합니다.

## 5. 아키텍처 및 구조

*   **패키지 구조**: 도메인별로 패키지를 구성하고, 각 도메인 내부에 `controller`, `service`, `repository`, `domain`, `dto` 등의 계층별 패키지를 가집니다.
    *   `com.github.copyinaction.[도메인명].domain`: 도메인 엔티티, Value Object, 도메인 서비스 인터페이스 등 핵심 비즈니스 로직 포함
    *   `com.github.copyinaction.[도메인명].repository`: Spring Data JPA Repository 인터페이스
    *   `com.github.copyinaction.[도메인명].service`: 비즈니스 로직을 구현하는 서비스 계층
    *   `com.github.copyinaction.[도메인명].controller`: REST API 엔드포인트 정의
    *   `com.github.copyinaction.[도메인명].dto`: 요청(Request) 및 응답(Response) 데이터 전송 객체
*   **DTO toEntity() 제거 및 Domain 팩토리 메서드 전환**: DTO가 도메인 엔티티 생성 로직을 가지는 대신, 도메인 엔티티 내부에 `create()`와 같은 팩토리 메서드를 두어 엔티티의 생성 책임을 도메인 자체로 이동시킵니다.
*   **풍부한 도메인 모델 (Rich Domain Model)**: 도메인 엔티티는 단순한 데이터 홀더(Anemic Domain Model)가 아닌, 비즈니스 로직과 행위를 포함하는 풍부한 객체여야 합니다.
    *   **비즈니스 로직은 도메인에**: Service 계층의 비즈니스 로직을 가능한 한 도메인 엔티티 내부로 이동시킵니다.
    *   **자기 캡슐화**: 엔티티의 상태 변경은 반드시 엔티티 내부 메서드를 통해서만 이루어져야 합니다. (예: `entity.updateName(newName)`)
    *   **불변식 보장**: 도메인 규칙과 검증 로직을 엔티티 내부에 캡슐화하여 항상 유효한 상태를 유지합니다.
    *   **예시**:
        ```kotlin
        // Bad: Service에서 직접 상태 변경
        fun updateVenue(id: Long, request: UpdateRequest) {
            val venue = repository.findById(id)
            venue.name = request.name  // 직접 접근
            venue.address = request.address
        }

        // Good: 도메인 메서드를 통한 상태 변경
        fun updateVenue(id: Long, request: UpdateRequest) {
            val venue = repository.findById(id)
            venue.update(name = request.name, address = request.address)  // 도메인 메서드 호출
        }
        ```

## 6. DTO 설계 및 Validation

*   **DTO 네이밍 컨벤션**:
    *   요청 DTO: `Create[도메인]Request`, `Update[도메인]Request`
    *   응답 DTO: `[도메인]Response`
    *   파일명: 도메인별로 `[도메인]Dto.kt` 파일에 관련 DTO 클래스들을 모아서 정의
*   **Response DTO 변환 메서드**: 엔티티 → DTO 변환은 Response DTO의 `companion object`에 `from()` 메서드로 정의합니다.
    ```kotlin
    data class VenueResponse(
        val id: Long,
        val name: String
    ) {
        companion object {
            fun from(venue: Venue): VenueResponse {
                return VenueResponse(
                    id = venue.id,
                    name = venue.name
                )
            }
        }
    }
    ```
*   **Validation 규칙**:
    *   `@field:` prefix를 사용하여 필드 레벨 검증을 명시합니다.
    *   검증 메시지는 한글로 작성하여 사용자 친화적인 에러 메시지를 제공합니다.
    ```kotlin
    data class CreateVenueRequest(
        @field:NotBlank(message = "공연장 이름은 비워둘 수 없습니다.")
        @field:Size(max = 100, message = "공연장 이름은 100자를 초과할 수 없습니다.")
        @Schema(description = "공연장 이름", example = "올림픽홀", required = true)
        val name: String,

        @field:Size(max = 255, message = "주소는 255자를 초과할 수 없습니다.")
        @Schema(description = "주소", example = "서울시 송파구")
        val address: String? = null
    )
    ```

## 7. API 설계

*   **RESTful API**: RESTful 원칙을 준수하여 자원(Resource) 기반의 API를 설계합니다.
*   **HTTP 메서드 활용**: 각 HTTP 메서드(GET, POST, PUT, DELETE 등)의 의미에 맞게 사용합니다.
*   **응답 형식**: JSON을 기본 응답 형식으로 사용합니다.
*   **API 문서**: Springdoc OpenAPI를 사용하여 Swagger UI를 통해 API 문서를 자동 생성합니다.
    *   `@Tag`, `@Operation`, `@ApiResponse`, `@Schema` 등의 어노테이션을 사용하여 API 명세 및 예시를 명확하게 작성합니다.
    *   `@Tag`의 `name`은 Orval과 같은 코드 생성 도구에서 디렉토리명으로 사용될 수 있도록 소문자, 케밥 케이스(`kebab-case`) 형태로 간결하게 작성합니다. (예: `performance-schedule`, `venue`, `seat` 등)

## 8. 예외 처리

*   **ErrorCode enum**: 모든 에러 코드는 `ErrorCode` enum에 정의하며, 도메인별로 그룹화하여 관리합니다.
    ```kotlin
    enum class ErrorCode(
        val status: HttpStatus,
        val message: String,
    ) {
        // Common
        INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력 값이 올바르지 않습니다."),

        // Venue
        VENUE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공연장을 찾을 수 없습니다."),
        SEAT_CAPACITY_ALREADY_EXISTS(HttpStatus.CONFLICT, "해당 등급의 좌석 용량이 이미 존재합니다."),

        // Performance
        PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공연을 찾을 수 없습니다."),
    }
    ```
*   **CustomException 사용**: 비즈니스 예외는 `CustomException`을 사용하여 throw합니다.
    ```kotlin
    throw CustomException(ErrorCode.VENUE_NOT_FOUND)
    ```
*   **HTTP 상태 코드 매핑**:
    *   `NOT_FOUND (404)`: 리소스를 찾을 수 없음
    *   `BAD_REQUEST (400)`: 잘못된 요청/입력값
    *   `CONFLICT (409)`: 중복/충돌 발생
    *   `UNAUTHORIZED (401)`: 인증 실패
    *   `FORBIDDEN (403)`: 권한 없음

## 9. 보안

*   **인증/인가**: JWT(JSON Web Tokens)와 Spring Security를 사용하여 인증 및 인가를 처리합니다.
*   **JWT Secret**: `JWT Secret` 값은 환경 변수(`JWT_SECRET_LOCAL`, `JWT_SECRET_PROD`)를 통해 관리하며, Base64 인코딩된 문자열을 사용합니다.
*   **권한 부여**: `@PreAuthorize` 어노테이션을 사용하여 메서드 수준에서 권한을 제어합니다.
*   **공개 엔드포인트**: Swagger UI 관련 엔드포인트(`swagger-ui/**`, `v3/api-docs/**`) 및 인증 관련 엔드포인트(`api/auth/**`)는 별도의 인증 없이 접근 가능하도록 설정합니다.

## 10. 데이터베이스

*   **데이터베이스**: PostgreSQL
*   **ORM**: Spring Data JPA (Hibernate 구현체)
*   **스키마 관리**: `spring.jpa.hibernate.ddl-auto=update` 설정을 통해 엔티티 변경 시 스키마 자동 반영
*   **Auditing**: `BaseEntity`를 통해 생성/수정 시간 자동 기록 (`@EnableJpaAuditing` 활성화).
*   **엔티티 설계 규칙**:
    *   모든 엔티티는 `BaseEntity`를 상속하여 `createdAt`, `updatedAt` 필드를 자동 관리합니다.
    *   `@Comment` 어노테이션을 사용하여 DB 컬럼/테이블에 설명을 추가합니다.
    *   ID 필드는 기본값 `0`을 설정하여 영속화 전후 상태를 구분합니다.
    ```kotlin
    @Entity
    @Table(name = "venue")
    @Comment("공연장 정보")
    class Venue(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Comment("공연장 ID")
        val id: Long = 0,

        @Column(nullable = false, length = 100)
        @Comment("공연장 이름")
        var name: String
    ) : BaseEntity()
    ```
*   **트랜잭션 관리**:
    *   Service 클래스 레벨에 `@Transactional(readOnly = true)`를 기본으로 설정합니다.
    *   쓰기 작업(생성/수정/삭제)을 수행하는 메서드에만 `@Transactional`을 추가합니다.
    ```kotlin
    @Service
    @Transactional(readOnly = true)  // 클래스 레벨: 읽기 전용 기본
    class VenueService(
        private val venueRepository: VenueRepository
    ) {
        fun getVenue(id: Long): VenueResponse { ... }  // readOnly 상속

        @Transactional  // 쓰기 작업에만 추가
        fun createVenue(request: CreateVenueRequest): VenueResponse { ... }

        @Transactional
        fun updateVenue(id: Long, request: UpdateVenueRequest): VenueResponse { ... }

        @Transactional
        fun deleteVenue(id: Long) { ... }
    }
    ```

## 11. 설정 관리

*   **Spring Profiles**: `local`, `prod` 등의 프로파일을 사용하여 환경별 설정을 분리합니다.
*   **환경 변수**: 민감한 정보(데이터베이스 접속 정보, JWT Secret 등)는 환경 변수(`application-local.yml`, `application-prod.yml`의 `${VAR_NAME}`)를 통해 외부에서 주입받습니다.
*   `.env` 파일 지원: 로컬 개발 환경에서 `spring-dotenv`를 사용하여 `.env` 파일의 환경 변수를 로드할 수 있습니다.

## 12. 로깅

*   **로깅 프레임워크**: SLF4J (추상화 계층) + Logback (구현체)
*   **설정 파일**: `logback-spring.xml`을 통해 로깅 레벨, 출력 형식, 파일 저장 경로 등을 구성합니다.
*   **로그 경로**: Docker 환경에서는 `/home/cic/logs/smarter-store/`, 로컬 환경에서는 `./logs/smarter-store-local/`에 로그가 저장됩니다.

## 13. 테스트

*   **테스트 프레임워크**: JUnit 5
*   **모의 객체**: MockK (Kotlin 친화적인 mocking 라이브러리)
*   **통합 테스트**: `@SpringBootTest`를 활용하여 실제 스프링 컨텍스트를 로드하여 테스트합니다.
*   **단위 테스트**: Mocking을 활용하여 특정 계층의 로직만을 검증합니다.

## 14. Git 활용

*   `.gitattributes` / `.gitignore`: 버전 관리에서 제외할 파일 및 속성을 정의합니다.
*   **브랜치 전략**: Git Flow 기반 (`main`, `develop`, `feature/*`, `hotfix/*`)

### 커밋 메시지 규칙
Conventional Commits 형식을 따릅니다.

**형식**: `<type>: <subject>`

| type | 설명 |
|------|------|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 리팩토링 (기능 변경 없음) |
| `docs` | 문서 수정 |
| `style` | 코드 포맷팅, 세미콜론 누락 등 |
| `test` | 테스트 코드 추가/수정 |
| `chore` | 빌드, 설정 파일 수정 |

**예시**:
```
feat: VenueSeatCapacity CRUD API 추가
fix: 좌석 점유 만료 시간 계산 오류 수정
refactor: PerformanceSchedule 팩토리 메서드로 전환
docs: DEVELOPMENT_CONVENTION 문서 관리 섹션 추가
```

**작성 원칙**:
*   제목은 50자 이내, 명령형으로 작성 (예: "추가", "수정", "삭제")
*   제목 끝에 마침표(.) 사용하지 않음
*   본문이 필요한 경우 제목과 본문 사이에 빈 줄 추가
*   "무엇을" 변경했는지 명확하게 기술

## 15. 문서 관리

### 문서 위치
모든 프로젝트 문서는 `documents/` 폴더에서 관리합니다.

### 문서 종류

| 문서 | 파일 | 설명 |
|------|------|------|
| 개발 컨벤션 | `documents/DEVELOPMENT_CONVENTION.md` | 코딩 규칙, 아키텍처 패턴 |
| 기능 명세 | `documents/features/*.md` | 도메인별 기능 상세 설명 |
| 설정 가이드 | `documents/setup/*.md` | 환경 설정, 인프라 관련 |
| API 명세 | Swagger UI (`/swagger-ui.html`) | 자동 생성 API 문서 |
| 변경 이력 | `CHANGELOG.md` | 버전별 변경 사항 |

### 문서 업데이트 원칙
코드 변경 시 관련 문서도 함께 업데이트합니다.
*   API 변경 → Swagger 어노테이션(`@Operation`, `@Schema`, `@ApiResponse`) 즉시 업데이트
*   새 패턴/컨벤션 도입 → `DEVELOPMENT_CONVENTION.md` 업데이트
*   주요 기능 추가/변경 → 해당 기능 문서 업데이트

### 문서 작성 시 주의사항
*   마크다운 형식 사용
*   **문서 상단에 개정이력 테이블 작성** (버전, 날짜, 작성자, 변경 내용)
    ```markdown
    | 버전 | 날짜 | 작성자 | 변경 내용 |
    |------|------|--------|----------|
    | 1.0 | 2025-01-01 | 홍길동 | 초안 작성 |
    | 1.1 | 2025-01-15 | 홍길동 | OO 기능 추가 |
    ```
*   코드 예시는 실제 프로젝트 코드와 일치하도록 유지
*   더 이상 유효하지 않은 내용은 즉시 삭제 또는 수정

### CHANGELOG 작성 규칙
*   **작성 시점**: 당일 작업 완료 후 작성
*   **헤더 형식**: `## YYYY년 MM월 DD일 (요일)`
*   **내용 구조**: 기능 단위로 굵은 제목 + 1-2줄 설명
    ```markdown
    ## 2025년 12월 18일 (목)

    *   **VenueSeatCapacity CRUD 구현:**
        *   `VenueService`, `VenueController`에 등급별 좌석 용량 API 추가 (조회, 단건등록, 일괄설정, 삭제)
        *   `VenueSeatCapacity.create()` 팩토리 메서드 추가
    *   **DDD 리팩토링 - DTO toEntity() 제거:**
        *   `PerformanceSchedule.create()` 팩토리 메서드 구현
        *   `CreatePerformanceScheduleRequest.toEntity()` 제거, Service에서 팩토리 메서드 사용
    ```
*   **작성 원칙**:
    *   너무 상세하지 않게, 너무 간략하지 않게 (타이틀 + 핵심 내용 1-2줄)
    *   코드 변경의 "무엇을" 했는지 명확하게 기술
    *   관련 클래스/메서드명은 백틱(\`)으로 감싸서 표기

---
이 컨벤션 문서는 프로젝트의 요구사항과 기술 스택의 변화에 따라 지속적으로 업데이트될 수 있습니다.
