# Changelog

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
