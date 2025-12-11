# JWT 인증 전략 가이드

이 문서는 `smarter-store-api` 프로젝트의 JWT(JSON Web Token) 기반 인증 시스템 전체에 대해 설명합니다. Access Token과 Refresh Token을 사용하는 이중 토큰 방식을 기반으로, 보안 강화를 위해 `HttpOnly` 및 `Secure` 속성의 쿠키를 통해 토큰을 관리합니다.

---

## 1. 개요: 쿠키 기반 이중 토큰 전략

`localStorage`에 토큰을 저장하는 방식은 XSS(Cross-Site Scripting) 공격에 취약할 수 있습니다. 이 프로젝트에서는 이러한 보안 위협을 완화하기 위해 다음과 같은 전략을 사용합니다.

- **Access Token (단기, 1시간)**: 실제 API 인증에 사용됩니다. 수명이 짧아 탈취 시 피해를 최소화합니다.
- **Refresh Token (장기, 7일)**: Access Token을 재발급받는 데 사용됩니다.
- **`HttpOnly` 쿠키 저장**: 두 토큰 모두 JavaScript에서 접근할 수 없는 `HttpOnly` 쿠키에 저장하여 XSS 공격으로부터 토큰을 보호합니다.
- **`Secure` 및 `SameSite` 속성**: `Secure` 속성으로 HTTPS 통신을 강제하고, `SameSite` 속성으로 CSRF(Cross-Site Request Forgery) 공격을 방어합니다.

---

## 2. 전체 인증 흐름

### 2.1. 로그인 (`POST /api/auth/login`)

1.  사용자가 이메일과 비밀번호로 로그인을 요청합니다.
2.  서버는 인증 성공 시, **Access Token**과 **Refresh Token**을 생성합니다.
3.  생성된 토큰들은 응답의 `Set-Cookie` 헤더를 통해 브라우저에 쿠키로 설정됩니다.
    -   **Access Token 쿠키**: `Max-Age`가 1시간으로 설정됩니다.
    -   **Refresh Token 쿠키**: `Max-Age`가 7일로 설정됩니다. `refresh_tokens` DB 테이블에도 저장됩니다.

### 2.2. API 요청

1.  프론트엔드에서 인증이 필요한 API를 호출하면, 브라우저는 요청 시 자동으로 `Cookie` 헤더에 `accessToken`을 담아 전송합니다.
2.  백엔드의 `JwtAuthenticationFilter`가 요청을 받아 `accessToken` 쿠키를 검증합니다.
3.  토큰이 유효하면, Spring Security 컨텍스트에 사용자 인증 정보를 저장하고 API 접근을 허용합니다.

### 2.3. 토큰 갱신 (`POST /api/auth/refresh`)

1.  Access Token이 만료되면 API 요청은 `401 Unauthorized` 에러를 반환합니다.
2.  프론트엔드는 이 응답을 감지하고, `/api/auth/refresh` 엔드포인트로 토큰 갱신을 요청합니다. (이때 브라우저는 `refreshToken` 쿠키를 자동으로 함께 보냅니다.)
3.  서버는 `refreshToken` 쿠키 값과 DB에 저장된 토큰을 비교하여 유효성을 검증합니다.
4.  검증 성공 시, 새로운 Access Token(과 선택적으로 새로운 Refresh Token)을 발급하여 다시 `Set-Cookie` 헤더로 전달합니다. (토큰 순환, Token Rotation)

### 2.4. 로그아웃 (`POST /api/auth/logout`)

1.  프론트엔드가 로그아웃을 요청합니다.
2.  서버는 DB에 저장된 Refresh Token을 삭제하고, 응답 헤더에 두 쿠키의 `Max-Age`를 `0`으로 설정하여 브라우저에서 즉시 삭제되도록 합니다.

---

## 3. 토큰 상세 구조 및 특징

### 3.1. Access Token

-   **형식**: 표준 JWT
-   **Payload 예시**:
    ```json
    {
      "sub": "user@example.com", // 사용자 이메일
      "auth": "ROLE_USER",       // 권한
      "iat": 1701849600,         // 발급 시간
      "exp": 1701853200          // 만료 시간
    }
    ```

### 3.2. Refresh Token

-   **형식**: JWT가 아닌, 암호학적으로 안전한 랜덤 UUID 문자열.
-   **저장**: `refresh_tokens` 데이터베이스 테이블에 사용자 정보와 매핑되어 저장됩니다.
-   **토큰 순환 (Token Rotation)**: 보안 강화를 위해 토큰 갱신 요청 시, 기존 Refresh Token은 무효화되고 항상 새로운 Refresh Token이 발급됩니다.

---

## 4. 보안 고려사항

-   **JWT Secret**: 256비트 이상의 강력한 키를 사용하며, 환경 변수(`JWT_SECRET`)를 통해 주입받습니다.
-   **HTTPS 필수**: `Secure` 쿠키 속성을 통해 암호화되지 않은 HTTP 통신에서의 토큰 탈취를 원천적으로 차단합니다.
-   **XSS 방어**: `HttpOnly` 쿠키 속성으로 JavaScript를 통한 토큰 탈취를 방지합니다.
-   **CSRF 방어**: `SameSite=Lax` 또는 `Strict` 쿠키 속성을 통해 다른 출처에서의 요청에 쿠키가 전송되는 것을 제어하여 CSRF 공격을 완화합니다.
-   **관리자 계정**: 관리자 계정은 보안 강화를 위해 Refresh Token을 사용하지 않고, Access Token이 만료될 때마다 재로그인을 요구할 수 있습니다.

---

## 5. 프론트엔드 구현 가이드

-   **토큰 직접 저장 금지**: `localStorage`나 `sessionStorage`에 토큰을 저장하지 마십시오. 모든 토큰 관리는 브라우저의 쿠키 메커니즘을 통해 자동으로 이루어집니다.
-   **API 요청 시 `credentials` 설정**: API 요청 시 쿠키를 함께 보내기 위해 `credentials: 'include'` (Fetch API) 또는 `withCredentials: true` (Axios) 옵션을 **반드시** 설정해야 합니다.
-   **`Authorization` 헤더 불필요**: 쿠키를 사용하므로, `Authorization: Bearer <token>` 헤더를 수동으로 추가할 필요가 없습니다.
-   **401 응답 처리**: Access Token 만료로 인한 `401 Unauthorized` 응답을 처리하는 로직(예: 토큰 갱신 요청)을 구현해야 합니다.

---

## 6. 백엔드 관련 파일

| 파일 | 설명 |
|:---|:---|
| `config/jwt/JwtTokenProvider.kt` | Access Token 생성, 검증, 정보 추출 |
| `config/jwt/JwtAuthenticationFilter.kt` | 모든 요청에서 쿠키를 읽어 토큰을 검증하고 인증 컨텍스트를 설정하는 필터 |
| `auth/service/CookieService.kt` | `HttpOnly`, `Secure` 등 보안 속성이 적용된 쿠키 생성 및 관리를 중앙에서 처리 |
| `auth/service/AuthService.kt` | 로그인, 로그아웃, 토큰 갱신 등 핵심 인증 로직 |
| `auth/domain/RefreshToken.kt` | Refresh Token 엔티티 |
| `auth/repository/RefreshTokenRepository.kt` | Refresh Token DB 작업을 위한 Repository |
