# 보안 가이드: Spring Security 및 JWT 기반 인증/인가

이 문서는 프로젝트에 적용된 Spring Security 및 JWT(JSON Web Token) 기반 인증/인가 시스템에 대해 상세히 설명합니다.

## 1. 개요

프로젝트는 Spring Security 프레임워크와 JWT를 활용하여 REST API를 보호합니다. 이는 클라이언트-서버 간의 인증 및 인가를 효율적이고 안전하게 처리하기 위함입니다. 핵심은 서버가 세션을 유지하지 않는(Stateless) 방식으로 확장성과 유연성을 높이는 것입니다.

## 2. 주요 구성 요소

### 2.1. 의존성

`build.gradle.kts` 파일에 `spring-boot-starter-security`와 `jjwt` 라이브러리가 추가되어 있습니다.

### 2.2. `User` 엔티티 및 `Role` Enum (`domain` 패키지)

*   **`User` 엔티티**: 사용자 인증 정보(이메일, 암호화된 비밀번호, 역할)를 저장하는 데이터베이스 엔티티입니다.
*   **`Role` Enum**: `USER`, `ADMIN` 등 사용자의 권한을 정의하는 열거형(Enum)입니다.

### 2.3. `JwtTokenProvider` (`config/jwt/JwtTokenProvider.kt`)

*   **역할**: JWT Access Token의 생성, 토큰 내 사용자 정보(Claim) 파싱, 토큰 유효성 검증을 담당하는 핵심 컴포넌트입니다.
*   **설정**: 토큰의 만료 시간, 토큰 서명에 사용되는 시크릿 키는 `application.yml`에서 설정됩니다.

### 2.4. `JwtAuthenticationFilter` (`config/jwt/JwtAuthenticationFilter.kt`)

*   **역할**: Spring Security 필터 체인에 등록되어 모든 HTTP 요청을 가로챕니다. `Authorization` 헤더에서 JWT를 추출하고, `JwtTokenProvider`를 통해 토큰의 유효성을 검증합니다.
*   **동작**: 유효한 토큰일 경우, 토큰 내의 사용자 정보를 기반으로 `SecurityContextHolder`에 인증 정보를 설정하여 해당 요청이 인증된 것으로 처리되도록 합니다.

### 2.5. `SecurityConfig` (`config/SecurityConfig.kt`)

*   **역할**: 프로젝트의 전반적인 Spring Security 설정을 정의하는 클래스입니다.
*   **주요 설정**:
    -   **`@EnableWebSecurity`**: Spring Security의 웹 보안 기능을 활성화합니다.
    -   **`@EnableMethodSecurity`**: `@PreAuthorize` 어노테이션을 사용하여 메서드 수준의 세밀한 접근 제어를 가능하게 합니다.
    -   **`PasswordEncoder`**: 비밀번호 암호화를 위해 `BCryptPasswordEncoder`를 빈(Bean)으로 등록하여 사용합니다.
    -   **인가 규칙**:
        -   `/api/auth/**` (인증 관련 API: 로그인, 회원가입), `/swagger-ui/**`, `/v3/api-docs/**` (Swagger 문서) 등 특정 경로에 대해서는 인증 없이 접근을 허용(`permitAll()`)합니다.
        -   그 외의 모든 요청(`anyRequest()`)은 반드시 인증(`authenticated()`)되어야 접근할 수 있도록 설정합니다.
    -   **`JwtAuthenticationFilter` 등록**: `UsernamePasswordAuthenticationFilter` 앞에 `JwtAuthenticationFilter`를 추가하여 JWT 기반 인증이 먼저 수행되도록 합니다.
    -   **세션 관리**: REST API는 서버에 사용자 세션 상태를 저장하지 않으므로, `SessionCreationPolicy.STATELESS`로 설정합니다.

### 2.6. `CustomUserDetailsService` (`service/CustomUserDetailsService.kt`)

*   **역할**: Spring Security가 사용자 로그인 요청 시 `UserRepository`를 통해 데이터베이스에서 사용자 정보를 로드하는 데 사용됩니다. `UserDetails` 인터페이스를 구현합니다.

### 2.7. `AuthController` (`controller/AuthController.kt`)

*   **역할**: 사용자 인증과 관련된 API 엔드포인트(예: 회원가입, 로그인)를 제공합니다.
*   **주요 엔드포인트**:
    -   `/api/auth/signup`: 새로운 사용자를 등록합니다.
    -   `/api/auth/login`: 유효한 이메일과 비밀번호로 로그인하여 JWT Access Token을 발급받습니다.

### 2.8. `@PreAuthorize` (메서드 수준 보안)

*   **역할**: Controller 계층의 특정 API 메서드에 `@PreAuthorize("hasRole('ROLE_ADMIN')")`와 같은 어노테이션을 직접 적용하여, 해당 메서드를 호출할 수 있는 사용자 권한을 제한합니다. (예: `ProductController` 참조)

## 3. JWT 토큰의 `application.yml` 설정 예시

JWT 관련 설정은 `application.yml` 파일에서 관리됩니다.

```yaml
jwt:
  secret: "ReplaceThisWithYourOwnSecretKeyThatIsLongAndSecureEnough" # 실제 운영에서는 환경 변수 등을 통해 주입해야 함
  access-token-validity-in-seconds: 3600 # Access Token 유효 시간 (초 단위, 예: 1시간)
```

## 4. JWT 라이프사이클 및 사용 방법

JWT 기반 인증 시스템의 전체적인 흐름은 다음과 같습니다.

### 4.1. 발급 (Issuance)

-   사용자가 `/api/auth/login` 엔드포인트로 유효한 이메일과 비밀번호를 전송합니다.
-   서버는 이 정보를 검증하고, 성공 시 `JwtTokenProvider`를 사용하여 사용자 정보를 담은 JWT Access Token을 생성합니다.
-   생성된 Access Token은 `TokenResponse` DTO에 담겨 클라이언트에 반환됩니다.

### 4.2. 클라이언트의 토큰 저장 및 사용 (Client-side Storage & Usage)

-   클라이언트(웹 브라우저의 로컬 스토리지/세션 스토리지, 모바일 앱의 안전한 저장소 등)는 발급받은 Access Token을 안전하게 저장합니다.
-   보호된 API에 접근할 때마다, 클라이언트는 HTTP 요청 헤더에 `Authorization: Bearer <Access Token>` 형식으로 Access Token을 포함하여 전송합니다.

### 4.3. 서버의 토큰 검증 (Server-side Validation)

-   모든 보호된 API 요청은 Spring Security 필터 체인에 등록된 `JwtAuthenticationFilter`를 통과합니다.
-   `JwtAuthenticationFilter`는 요청 헤더에서 Access Token을 추출하고, `JwtTokenProvider`를 사용하여 토큰의 유효성을 검증합니다. (서명 일치 여부, 만료 여부 등)
-   유효한 토큰인 경우, 토큰 내의 사용자 정보(subject, 권한)를 추출하여 `SecurityContextHolder`에 `Authentication` 객체를 설정합니다. 이로써 해당 요청은 Spring Security 컨텍스트 내에서 인증된 것으로 처리됩니다.

### 4.4. 인가 처리 (Authorization)

-   `SecurityContextHolder`에 인증 정보가 설정되면, `@PreAuthorize` 어노테이션이 붙은 메서드가 호출될 때 Spring Security가 현재 사용자가 해당 리소스에 접근할 권한이 있는지 확인합니다.

### 4.5. 만료 (Expiration)

-   Access Token은 만료 시간을 가지고 있습니다 (`access-token-validity-in-seconds`로 설정).
-   만료된 토큰으로 요청 시, `JwtAuthenticationFilter`의 `jwtTokenProvider.validateToken()`에서 예외가 발생하고, 서버는 `401 Unauthorized` 또는 `403 Forbidden` 등의 응답을 반환합니다.
-   현재 프로젝트에는 Refresh Token 메커니즘이 구현되어 있지 않지만, 실제 운영 환경에서는 Access Token 만료 시 Refresh Token을 사용하여 새로운 Access Token을 발급받아 사용자 경험을 유지하는 것이 일반적입니다.

이러한 라이프사이클을 통해 클라이언트는 서버에 로그인 상태를 유지하면서 안전하게 API에 접근할 수 있습니다.

## 5. Access Token과 Refresh Token

보안성과 사용자 경험을 동시에 고려할 때, JWT 시스템은 흔히 두 가지 토큰을 함께 사용합니다: **Access Token**과 **Refresh Token**.

### 5.1. Access Token (접근 토큰)

-   **역할**: 보호된 API 리소스에 접근하기 위해 사용됩니다. 실제 권한 부여를 위한 주된 토큰입니다.
-   **특징**:
    -   **단기성**: 보안을 위해 유효 기간을 짧게(예: 30분 ~ 1시간) 설정합니다. 탈취되더라도 노출 시간이 짧습니다.
    -   **무상태(Stateless)**: 서버는 Access Token 자체를 별도로 저장하거나 관리하지 않습니다. 토큰 내부의 정보(서명)만으로 유효성을 검증합니다. 이는 서버의 확장성(Scalability)에 큰 장점을 제공합니다.
    -   **탈취 위험**: 만약 탈취된다면 만료 시간까지는 권한이 악용될 수 있습니다.

### 5.2. Refresh Token (갱신 토큰)

-   **역할**: Access Token이 만료되었을 때, 사용자에게 재로그인을 요구하지 않고 새로운 Access Token을 발급받기 위해 사용됩니다.
-   **특징**:
    -   **장기성**: Access Token보다 긴 유효 기간(예: 1주 ~ 수개월)을 가집니다.
    -   **상태 관리 필요**: 보안을 위해 서버에서 Refresh Token의 유효성을 관리해야 합니다 (저장, 만료, 취소). 이는 Refresh Token이 탈취될 경우, 서버에서 해당 Refresh Token을 무효화하여 Access Token 재발급을 막기 위함입니다. 따라서 Access Token과 달리 **상태를 가집니다 (Stateful)**.
    -   **사용 목적 한정**: Access Token처럼 직접 API 리소스에 접근하는 데 사용되지 않고, 오직 새로운 Access Token을 발급받는 데만 사용됩니다.

### 5.3. Access/Refresh Token을 이용한 로그인 및 사용 흐름

1.  **초기 로그인**:
    -   사용자가 ID/PW로 로그인 성공 시, 서버는 **Access Token**과 **Refresh Token**을 모두 발급하여 클라이언트에 전달합니다.
    -   클라이언트는 Access Token을 메모리나 안전한 곳(HTTP-only 쿠키)에 저장하여 매 요청 시 `Authorization` 헤더에 포함합니다.
    -   Refresh Token은 Access Token보다 더 안전한 곳(예: **HTTP-only Secure Cookie**)에 저장합니다. 자바스크립트 접근을 막아 XSS 공격에 대한 위험을 줄입니다.

2.  **API 요청**:
    -   클라이언트는 Access Token을 포함하여 보호된 API를 호출합니다.
    -   서버는 Access Token의 유효성을 검증하고, 유효하면 API 요청을 처리합니다.

3.  **Access Token 만료 시**:
    -   Access Token이 만료되면, 서버는 `401 Unauthorized` 응답을 반환합니다.
    -   클라이언트는 저장해 둔 Refresh Token을 사용하여 서버의 특정 엔드포인트(예: `/api/auth/refresh`)로 새로운 Access Token 발급을 요청합니다.

4.  **토큰 갱신 (Refresh)**:
    -   서버는 Refresh Token을 수신하여 유효성을 검증합니다 (토큰 자체의 유효성, DB에 저장된 Refresh Token과의 일치 여부, 만료 여부).
    -   유효한 Refresh Token이라면, 서버는 **새로운 Access Token** (그리고 보안 강화를 위해 새로운 Refresh Token)을 발급하여 클라이언트에 전달합니다.
    -   클라이언트는 새로운 토큰으로 기존 토큰을 교체하고 API 요청을 재시도합니다.

### 5.4. 데이터베이스 구조 (Refresh Token 관리를 위한 예시)

Refresh Token의 상태를 서버에서 관리하기 위해 별도의 테이블이 필요합니다.

**`refresh_tokens` 테이블 예시:**
```sql
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,              -- User 테이블과의 외래 키
    token VARCHAR(255) NOT NULL UNIQUE,   -- Refresh Token 값 (암호화/해싱 가능)
    expiry_date TIMESTAMP NOT NULL,       -- 토큰 만료일
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
```

### 5.5. Redis와 같은 세션 관리 서버의 필요성 검토

-   **Access Token만 사용하는 경우**: Redis와 같은 세션 관리 서버는 **전혀 필요 없습니다**. Access Token은 무상태(Stateless)이므로 서버에서 별도의 정보를 저장할 필요가 없습니다.
-   **Refresh Token을 사용하는 경우**: Refresh Token은 서버에서 관리되어야 하므로, 상태 저장(Stateful)의 필요성이 생깁니다.
    -   **DB 사용**: 위에서 제안한 `refresh_tokens` 테이블처럼 데이터베이스에 Refresh Token을 저장하여 관리할 수 있습니다. 초기 단계나 트래픽이 많지 않은 경우 충분히 좋은 방법입니다.
    -   **Redis 사용**: Redis는 인메모리 데이터베이스로, DB보다 훨씬 빠른 읽기/쓰기 성능을 제공합니다. Refresh Token의 검증 및 무효화가 빈번하게 발생하거나, 고가용성 및 고성능이 요구되는 환경에서는 Redis를 사용하는 것이 유리합니다.

**권장 사항**: 초기 구현에서는 Refresh Token을 **데이터베이스에 저장**하는 방식으로 시작하는 것이 좋습니다. Redis는 추가적인 인프라 설정과 복잡도를 야기하므로, 서비스 규모가 커지거나 성능 요구사항이 높아질 때 도입을 고려하는 것을 추천합니다.
