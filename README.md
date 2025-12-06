# Gemini 컨텍스트: Smarter Store API

이 문서는 Gemini AI 어시스턴트가 `Smarter Store API` 프로젝트를 이해하는 데 필요한 포괄적인 개요를 제공합니다. 이 정보는 프로젝트 설정 가이드와 빌드 파일을 기반으로 작성되었습니다.

## 1. 프로젝트 개요

Smarter Store API는 Kotlin과 Spring Boot로 구축된 백엔드 REST API 서버입니다. 이 프로젝트는 테스트 주도 개발(TDD) 및 도메인 주도 설계(DDD) 원칙을 따릅니다.

이 프로젝트는 초기 설정 단계에 있으며, 점진적으로 다양한 기능과 기술 스택을 도입하고 있습니다.

## 2. 프로젝트 초기 문서 (Initial Project Documents)

*   **[INITIAL_SETUP.md](documents/INITIAL_SETUP.md)**: 이 문서는 프로젝트의 최초 생성 시점의 요구사항, 초기 기술 스택, 기본적인 설정 가이드 등을 담고 있습니다. 프로젝트의 시작점과 초기 방향성을 이해하는 데 도움을 줍니다.
*   **[GitHub Actions Docker 배포 가이드](documents/GITHUB_ACTIONS_DOCKER_DEPLOYMENT_GUIDE.md)**: GitHub Actions를 활용한 Docker 기반 서버 배포에 대한 상세 가이드입니다.

## 3. 적용 기술 스택 (Applied Technology Stack)

이 프로젝트는 다음과 같은 핵심 기술 스택과 방법론을 사용하여 개발되었습니다.

*   **언어**: Kotlin
*   **프레임워크**: Spring Boot 3.3.0 (Spring Security, Spring Data JPA 포함)
*   **빌드 도구**: Gradle (Kotlin DSL)
*   **데이터베이스**: PostgreSQL 18
*   **데이터베이스 마이그레이션**: Flyway
*   **인증/인가**: JWT (JJWT 라이브러리)
*   **API 문서화**: Springdoc OpenAPI (Swagger UI)
*   **요청 데이터 검증**: Jakarta Validation
*   **로깅**: SLF4J + Logback (날짜/용량 기반 롤링 정책)
*   **개발 방법론/개념**: DDD (도메인 주도 설계), TDD (테스트 주도 개발), RESTful API 디자인, 전역 예외 처리

## 4. 빌드 및 실행

### 4.1. 빌드

프로젝트를 빌드하려면 표준 Gradle `build` 명령어를 실행합니다.

```bash
./gradlew build
```

### 4.2. 실행

애플리케이션을 로컬에서 실행하려면 `bootRun` Gradle 태스크를 사용합니다. 애플리케이션은 `localhost:8080`에서 시작됩니다.

```bash
./gradlew bootRun
```

### 4.3. 테스트

테스트를 실행하려면 `test` Gradle 태스크를 사용합니다.

```bash
./gradlew test
```

## 5. 개발 규칙 및 컨벤션

### 5.1. 아키텍처 및 방법론

- 프로젝트는 **도메인 주도 설계(DDD)**를 따릅니다. 핵심 로직은 `domain` 패키지에, 비즈니스 로직은 `service`에, 외부와의 통신은 `controller`에 위치시켜 각 계층의 책임을 명확히 분리합니다.
- 모든 새로운 기능은 **테스트 주도 개발(TDD)** 접근 방식을 사용하여 개발되어야 합니다.

### 5.2. 계층별 구조 (Product 및 User/Auth 도메인 예시)

프로젝트는 DDD(도메인 주도 설계) 원칙에 따라 Controller, Service, DTO, Domain/Entity, Repository 계층으로 구성됩니다. 각 계층의 상세한 역할과 책임, 그리고 `Product` 및 사용자/인증 도메인의 예시는 [아키텍처 및 계층별 구조 가이드 문서](documents/ARCHITECTURE_DDD_GUIDE.md)를 참조해 주세요.

### 5.3. JPA Auditing (생성/수정일자 자동화)

프로젝트는 Spring Data JPA의 Auditing 기능을 사용하여 엔티티의 생성(`createdAt`) 및 수정(`updatedAt`) 일자를 자동으로 관리합니다. 설정 방법 및 상세 내용은 [JPA Auditing 가이드 문서](documents/JPA_AUDITING_GUIDE.md)를 참조해 주세요.

### 5.4. 전역 예외 처리 (`exception` 패키지)

프로젝트는 `@RestControllerAdvice`를 활용한 전역 예외 처리 메커니즘을 통해 클라이언트에게 일관된 형식의 에러 응답을 제공합니다. `ErrorCode`, `ErrorResponse`, `CustomException`, `GlobalExceptionHandler`로 구성됩니다. 상세 내용은 [전역 예외 처리 가이드 문서](documents/EXCEPTION_HANDLING_GUIDE.md)를 참조해 주세요.

### 5.5. 데이터베이스 스키마 관리 (Flyway)

이 프로젝트는 데이터베이스 스키마의 버전 관리를 위해 **Flyway**를 사용합니다. `spring.jpa.hibernate.ddl-auto: none`으로 설정되어 있어 JPA(Hibernate)의 자동 스키마 관리가 비활성화되고, Flyway가 스키마 변경을 전적으로 담당합니다.

*   `src/main/resources/db/migration` 경로에 `V{버전}__{설명}.sql` 형식의 SQL 파일을 추가하여 테이블 생성 및 변경을 관리합니다.
*   애플리케이션 실행 시, Flyway가 현재 데이터베이스 스키마 버전과 마이그레이션 파일들을 비교하여 자동으로 최신 상태로 업데이트합니다.

**Flyway의 상세한 개념, 동작 방식, 그리고 현재 프로젝트에 적용된 마이그레이션 스크립트 목록은 [Flyway 가이드 문서](documents/FLYWAY_GUIDE.md)를 참조해 주세요.**

### 5.6. API 문서 자동화 (Swagger / OpenAPI)

프로젝트는 `springdoc-openapi` 라이브러리를 사용하여 API 문서를 자동으로 생성합니다. 애플리케이션 실행 후 `http://localhost:8080/swagger-ui/index.html`에서 문서를 확인할 수 있습니다. 상세한 어노테이션 사용법 및 예시는 [Swagger/OpenAPI 가이드 문서](documents/SWAGGER_OPENAPI_GUIDE.md)를 참조해 주세요.

### 5.7. RESTful API 설계

프로젝트는 RESTful API 설계 원칙을 따릅니다. HTTP 메서드별 응답 규칙, 상태 코드 등은 [RESTful API 가이드 문서](documents/RESTFUL_API_GUIDE.md)를 참조해 주세요.

### 5.8. 보안 (Security)

이 프로젝트는 Spring Security와 JWT(JSON Web Token)를 사용하여 API를 보호합니다. 상세한 설정 및 사용 방법은 [보안 가이드 문서](documents/SECURITY_GUIDE.md)를 참조해 주세요.

*   **[JWT 토큰 가이드](documents/JWT_TOKEN_GUIDE.md)**: Access Token / Refresh Token 발급, 갱신, 인증 흐름에 대한 상세 가이드입니다.

### 5.9. 테스트

- **단위 및 통합 테스트**: 테스트 구조화에는 JUnit 5를 사용합니다.
- **모킹**: 단위 테스트에서 Mock 객체 생성 및 상호 작용 검증에는 `MockK`를 사용합니다.
- **테스트 위치**: 모든 테스트 파일은 `src/test/kotlin/`에 위치합니다.

## 6. 로깅

자세한 로깅 설정 및 정책은 [로깅 가이드 문서](documents/LOGGING_GUIDE.md)를 참조해 주세요.

## 7. 개발 이력 (Development Log)

자세한 개발 이력은 [CHANGELOG.md](CHANGELOG.md) 파일을 참조해 주세요.

