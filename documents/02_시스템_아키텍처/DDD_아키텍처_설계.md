# DDD 아키텍처 및 계층별 구조 설계

## 개정이력

| 버전 | 일자 | 작성자 | 내용 |
|------|------|--------|------|
| 1.0 | 2025-12-29 | Gemini | 초기 아키텍처 가이드 수립 |

---

이 문서는 프로젝트의 도메인 주도 설계(DDD) 기반 아키텍처와 계층별 구조에 대해 상세히 설명합니다.

## 1. 아키텍처 및 방법론

-   프로젝트는 **도메인 주도 설계(DDD)**와 **Rich Domain Model** 패턴을 따릅니다.
-   **비즈니스 로직은 Domain(Entity)에 위치**하고, Service는 도메인 메서드를 호출하여 유스케이스를 조율합니다.
-   모든 새로운 기능은 **테스트 주도 개발(TDD)** 접근 방식을 사용하여 개발되어야 합니다.

### 1.1. Rich Domain Model 패턴

**Domain에 구현하는 것:**
-   **팩토리 메서드**: `Entity.create()` - 객체 생성 로직 캡슐화
-   **비즈니스 검증**: `validatePassword()`, `canReserve()` 등
-   **상태 변경**: `confirm()`, `cancel()`, `verifyEmail()` 등
-   **계산 로직**: `calculateTotalPrice()` 등

**Service에 남기는 것:**
-   단순 조회/삭제 (Repository 호출)
-   트랜잭션 조율
-   외부 서비스 호출 (JWT 발급, 이메일 전송 등)
-   여러 도메인 간 협력 조율

### 1.2. Aggregate Root 패턴

부모-자식 관계의 엔티티는 Aggregate Root를 통해 관리합니다.

### 1.3. 코드 예시 (Before vs After)

**Rich Domain Model 적용 후:**
```kotlin
// Domain에서 로직 처리
class User {
    companion object {
        fun create(email: String, rawPassword: String, passwordEncoder: PasswordEncoder): User {
            return User(email = email, passwordHash = passwordEncoder.encode(rawPassword))
        }
    }
}

// Service는 도메인 메서드 호출
fun signup(request: SignupRequest): User {
    val user = User.create(request.email, request.password, passwordEncoder)
    return userRepository.save(user)
}
```

## 2. 기능/도메인별 패키지 구조

프로젝트는 특정 기능과 관련된 모든 요소(Controller, Service, Repository, Domain, DTO)가 하나의 상위 도메인 패키지 아래에 응집된 구조를 가집니다.

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
    ```

*   **각 서브패키지의 역할:**
    -   **`controller`**: HTTP 요청 처리 및 응답 반환.
    -   **`domain`**: 핵심 엔티티 및 비즈니스 로직.
    -   **`dto`**: API 스펙 및 데이터 전송 객체.
    -   **`repository`**: 영속성 관리.
    -   **`service`**: 도메인 협력 조율 및 트랜잭션 관리.

## 3. 공통 모듈 (Shared Modules)

-   **`config`**: 애플리케이션 설정 및 보안 설정.
-   **`common.exception`**: 전역 예외 처리 및 에러 코드.
-   **`common.domain`**: 모든 엔티티의 공통 부모 (`BaseEntity`).