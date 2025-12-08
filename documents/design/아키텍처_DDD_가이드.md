# 아키텍처 및 계층별 구조 가이드 (DDD)

이 문서는 프로젝트의 도메인 주도 설계(DDD) 기반 아키텍처와 계층별 구조에 대해 상세히 설명합니다.

## 1. 아키텍처 및 방법론

-   프로젝트는 **도메인 주도 설계(DDD)**를 따릅니다. 핵심 로직은 `domain` 패키지에, 비즈니스 로직은 `service`에, 외부와의 통신은 `controller`에 위치시켜 각 계층의 책임을 명확히 분리합니다.
-   모든 새로운 기능은 **테스트 주도 개발(TDD)** 접근 방식을 사용하여 개발되어야 합니다.

## 2. 기능/도메인별 패키지 구조 (Feature/Domain-based Package Structure)

프로젝트는 도메인 주도 설계(DDD) 원칙을 강화하기 위해 기능(또는 바운디드 컨텍스트) 단위로 패키지 구조를 재편했습니다. 이제 특정 기능과 관련된 모든 요소(Controller, Service, Repository, Domain, DTO)가 하나의 상위 도메인 패키지 아래에 응집되어 있습니다. 이를 통해 코드의 응집도를 높이고 도메인 간의 결합도를 낮춰 유지보수성과 확장성을 향상시킵니다.

### 2.1. 도메인 모듈 (e.g., `auth`, `admin`, `venue`, `performance`)

각 도메인 모듈은 독립적인 기능을 수행하는 하나의 바운디드 컨텍스트로 간주됩니다.

*   **패키지 구조 예시 (`com.github.copyinaction.auth`):**
    ```
    com.github.copyinaction
    └── auth
        ├── controller
        │   └── AuthController.kt
        ├── domain
        │   ├── User.kt
        │   ├── Role.kt
        │   └── RefreshToken.kt
        ├── dto
        │   └── AuthDto.kt
        ├── repository
        │   ├── UserRepository.kt
        │   └── RefreshTokenRepository.kt
        └── service
            ├── AuthService.kt
            └── CustomUserDetailsService.kt
    ```

*   **각 서브패키지의 역할:**
    -   **`controller`**: 해당 도메인의 HTTP 요청 처리 및 응답 반환. 서비스 계층에 위임.
    -   **`domain`**: 해당 도메인의 핵심 엔티티, 값 객체, 애그리거트 루트 정의. (예: `User`, `Role`, `RefreshToken`은 `auth` 도메인의 핵심 요소)
    -   **`dto`**: 계층 간 데이터 전송 객체 정의. API 스펙 역할.
    -   **`repository`**: 해당 도메인 엔티티의 영속성 관리 (데이터베이스 접근).
    -   **`service`**: 해당 도메인의 비즈니스 로직 수행, 트랜잭션 관리.

### 2.2. 공통 모듈 (Shared Modules)

여러 도메인에서 공통적으로 사용되는 요소들은 별도의 최상위 패키지로 분리하여 관리합니다.

*   **`com.github.copyinaction.config`**:
    -   **역할**: 애플리케이션 전반에 걸친 공통 설정(`SecurityConfig`, `OpenApiConfig`) 및 JWT 관련 컴포넌트(`jwt` 서브패키지) 포함.
    -   **주요 파일**: `config/SecurityConfig.kt`, `config/OpenApiConfig.kt`, `config/jwt/JwtAuthenticationFilter.kt`, `config/jwt/JwtTokenProvider.kt`

*   **`com.github.copyinaction.exception`**:
    -   **역할**: 전역 예외 처리(`GlobalExceptionHandler`) 및 커스텀 예외 정의(`CustomException`, `ErrorCode`, `ErrorResponse`).
    -   **주요 파일**: `exception/CustomException.kt`, `exception/ErrorCode.kt`, `exception/ErrorResponse.kt`

*   **`com.github.copyinaction.domain`**:
    -   **역할**: 모든 엔티티가 상속받는 공통 기본 엔티티(`BaseEntity`) 포함.
    -   **주요 파일**: `domain/BaseEntity.kt`

---
