# Changelog

## 2025년 12월 16일 (화)

*   **로그인 API 개선:**
    *   로그인 성공 시 사용자 정보(`UserResponse`)를 응답 본문에 포함하도록 변경.
    *   `LoginResponse` DTO 추가 (token + user).
*   **Grafana 모니터링 스택 구성:**
    *   `micrometer-registry-prometheus` 의존성 추가.
    *   Actuator에 `prometheus`, `metrics` 엔드포인트 노출.
    *   로컬 테스트용 `monitoring/` Docker Compose 설정 추가 (Grafana, Prometheus, Loki, Promtail).
*   **쿠키 도메인 디버깅 로그 추가:**
    *   `CookieService`에 쿠키 도메인 설정값 확인용 로그 추가.


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