# JWT 쿠키 기반 관리 전략 가이드

이 문서는 프론트엔드에서 `localStorage` 대신 `HttpOnly` 및 `Secure` 쿠키를 사용하여 JWT(JSON Web Token)를 관리하는 전략에 대해 설명합니다. 이 방식은 보안을 강화하고 특정 다중 탭/창 문제를 완화하는 데 도움이 될 수 있습니다.

## 1. `localStorage` 사용 시의 문제점

`localStorage`는 동일한 도메인의 모든 탭/창에서 공유되며 JavaScript를 통해 쉽게 접근 가능합니다.

*   **XSS(Cross-Site Scripting) 취약점:** 악성 스크립트가 주입될 경우, `localStorage`에 저장된 JWT를 쉽게 탈취할 수 있어 보안에 취약합니다.
*   **공유 상태 관리의 복잡성:** 한 탭에서 로그아웃하면 모든 탭에서 인증 상태가 사라지는 등, 여러 탭에서의 상태 동기화 및 관리가 복잡할 수 있습니다.
*   **수동적인 토큰 갱신:** 만료된 토큰을 갱신하기 위한 로직을 프론트엔드에서 수동으로 구현해야 합니다.

## 2. 쿠키를 이용한 JWT 관리의 장점

`HttpOnly` 및 `Secure` 속성이 설정된 쿠키는 `localStorage`의 여러 문제를 해결합니다.

*   **XSS 방어 (`HttpOnly` 속성):**
    *   `HttpOnly` 쿠키는 클라이언트 측 JavaScript 코드(`document.cookie`)에서 접근할 수 없습니다.
    *   이는 XSS 공격자가 스크립트를 통해 JWT를 직접 탈취하는 것을 막아 보안을 크게 강화합니다.
*   **MITM(Man-in-the-Middle) 방어 (`Secure` 속성):**
    *   `Secure` 쿠키는 HTTPS 연결을 통해서만 전송됩니다.
    *   암호화되지 않은 HTTP 통신을 통한 토큰 탈취를 방지하여 중간자 공격으로부터 보호합니다.
*   **자동 전송:**
    *   브라우저는 동일한 도메인으로 보내는 모든 HTTP 요청에 관련 쿠키를 자동으로 포함하여 백엔드로 전송합니다. 프론트엔드 코드에서 수동으로 헤더에 토큰을 추가할 필요가 없습니다.
*   **자동 만료:**
    *   쿠키는 `Expires` 또는 `Max-Age` 속성을 통해 만료 시간을 설정할 수 있으며, 브라우저가 만료 시 자동으로 쿠키를 삭제합니다.

## 3. 쿠키 기반 JWT (Access Token + Refresh Token) 관리 방법

일반적으로 Access Token(단기)과 Refresh Token(장기)을 함께 사용하여 인증을 관리합니다.

### 3.1. 로그인 성공 시 (백엔드)

1.  사용자 로그인 성공 시, 백엔드 서버는 **Access Token**과 **Refresh Token**을 모두 생성합니다.
2.  백엔드는 이 토큰들을 HTTP 응답의 `Set-Cookie` 헤더를 통해 클라이언트(브라우저)에게 보냅니다.
    *   **Access Token 쿠키 설정 예시:**
        ```
        Set-Cookie: accessToken=<your_access_token>; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=3600
        ```
        *   `HttpOnly`: JavaScript 접근 불가
        *   `Secure`: HTTPS에서만 전송
        *   `SameSite=Lax` 또는 `Strict`: CSRF(Cross-Site Request Forgery) 방어 (권장)
        *   `Max-Age`: Access Token의 유효 기간과 동일하게 설정 (예: 1시간)
    *   **Refresh Token 쿠키 설정 예시:**
        ```
        Set-Cookie: refreshToken=<your_refresh_token>; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=604800
        ```
        *   `HttpOnly`, `Secure`: Access Token과 동일
        *   `SameSite=Strict`: CSRF 방어에 더 엄격
        *   `Max-Age`: Refresh Token의 유효 기간과 동일하게 설정 (예: 7일)

### 3.2. 인증이 필요한 API 요청 시 (브라우저와 백엔드)

로그인 시 쿠키가 설정된 이후, 인증이 필요한 API를 요청할 때의 흐름은 다음과 같이 브라우저와 백엔드가 자동으로 처리합니다.

1.  **브라우저의 쿠키 자동 전송:**
    *   프론트엔드에서 보호된 API(예: `GET /api/products`)를 호출하면, 브라우저는 요청을 보낼 때 **자동으로 `accessToken` 쿠키를 `Cookie` 요청 헤더에 담아 함께 전송합니다.**
    *   프론트엔드 개발자는 이 과정을 위해 별도의 코드를 작성할 필요가 없습니다. (단, `credentials: 'include'` 설정은 필요합니다.)

2.  **백엔드의 쿠키 자동 검증 (`JwtAuthenticationFilter.kt`):**
    *   백엔드 서버는 API 요청을 받으면 컨트롤러에 도달하기 전에 **`JwtAuthenticationFilter`**가 먼저 요청을 가로챕니다.
    *   `JwtAuthenticationFilter`는 `Cookie` 요청 헤더에서 `accessToken`을 찾아 그 값을 읽습니다.
    *   `accessToken`이 존재하고 유효하다면, 필터는 토큰을 검증하여 사용자 인증 정보를 생성하고, 이를 `SecurityContextHolder`에 저장합니다.
    *   Spring Security는 `SecurityContextHolder`에 인증 정보가 저장된 것을 확인하고, 해당 요청이 인증되었다고 판단하여 API 접근을 허용합니다.

### 3.3. Access Token 만료 시 (백엔드와 프론트엔드 협업)

1.  프론트엔드가 만료된 Access Token으로 API 요청을 보내면 백엔드는 `401 Unauthorized` 응답을 보냅니다.
2.  프론트엔드는 이 `401` 응답을 받으면, Access Token 재발급을 위한 새로운 요청(`POST /api/auth/refresh` 등)을 백엔드로 보냅니다.
3.  백엔드는 이 요청을 받으면 브라우저가 자동으로 보낸 `HttpOnly` Refresh Token 쿠키를 확인합니다.
4.  Refresh Token이 유효하다면, 백엔드는 새로운 Access Token과 (선택적으로) 새로운 Refresh Token을 발급하고, 다시 `Set-Cookie` 헤더를 통해 브라우저에 보냅니다.
5.  프론트엔드는 재발급된 Access Token을 사용하여 원래의 API 요청을 다시 시도합니다.

### 3.4. 로그아웃 시 (백엔드)

1.  프론트엔드가 로그아웃 요청을 백엔드로 보냅니다.
2.  백엔드는 해당 Refresh Token을 무효화하고 (데이터베이스 등에서), `Set-Cookie` 헤더를 통해 Access Token과 Refresh Token 쿠키의 만료 시간을 과거로 설정하여 브라우저에서 강제로 삭제하도록 합니다.

## 4. 구현 시 고려사항

*   **CORS 설정:** 쿠키를 사용하는 경우 `Access-Control-Allow-Credentials: true`를 포함하여 CORS 설정을 올바르게 구성해야 합니다.
*   **RefreshToken 무효화:** RefreshToken 탈취 시 재사용을 막기 위해 서버 측에서 무효화 목록(blacklist)을 관리하는 것이 좋습니다.
*   **만료 시간 관리:** Access Token은 짧게, Refresh Token은 길게 설정하여 보안과 사용자 편의성을 동시에 고려합니다.
*   **다중 탭/창:** `HttpOnly` 쿠키 사용 시에도 여전히 모든 탭/창은 동일한 쿠키를 공유합니다. 따라서 한 탭에서 로그아웃하면 모든 탭에서 인증이 풀리는 것은 마찬가지입니다. 하지만 XSS 공격으로부터 토큰을 보호하여 전반적인 보안을 크게 강화합니다.
*   **Cross-site 쿠키 (localhost 개발 환경):** 프론트엔드 개발자가 `localhost`에서 개발서버 API에 직접 연결하는 경우, cross-site 쿠키 문제가 발생할 수 있습니다. 이를 해결하기 위해 백엔드에서 요청의 `Origin` 헤더를 확인하여 동적으로 쿠키 속성을 설정합니다:
    *   **localhost/127.0.0.1 요청 시:** `SameSite=None`, `Secure=false`
    *   **그 외 도메인 요청 시:** `SameSite=Lax/Strict`, `Secure=true`

## 5. 프론트엔드 개발자를 위한 참고사항

쿠키 기반 JWT 전략을 사용할 때 프론트엔드 개발자가 특별히 고려해야 할 사항은 다음과 같습니다.

*   **토큰 저장 금지**: `localStorage`, `sessionStorage` 등 클라이언트 측 스토리지에 Access Token 또는 Refresh Token을 직접 저장하지 마십시오. 토큰은 브라우저의 쿠키 저장소에 안전하게 보관됩니다.
*   **API 요청 시 헤더 설정 불필요**: 브라우저는 `Set-Cookie` 헤더를 통해 저장된 쿠키를 동일한 도메인으로 보내는 모든 API 요청에 자동으로 포함합니다. 따라서 `Authorization: Bearer <token>`과 같은 헤더를 수동으로 설정할 필요가 없습니다.
*   **CORS `credentials` 설정 필수**: 백엔드가 `Access-Control-Allow-Credentials: true`를 보내므로, 프론트엔드에서도 API 요청 시 `credentials: 'include'` (Fetch API) 또는 `withCredentials: true` (Axios, jQuery.ajax 등) 옵션을 반드시 설정해야 합니다. 이 옵션이 없으면 브라우저는 쿠키를 함께 보내지 않습니다.
    *   **Fetch API 예시:**
        ```javascript
        fetch('/api/protected-resource', {
            method: 'GET',
            credentials: 'include' // 이 설정이 필수입니다.
        });
        ```
    *   **Axios 예시:**
        ```javascript
        axios.defaults.withCredentials = true; // 전역 설정
        // 또는 특정 요청에만
        axios.get('/api/protected-resource', {
            withCredentials: true
        });
        ```
*   **401 Unauthorized 응답 처리**: Access Token이 만료되면 백엔드에서 `401 Unauthorized` 응답을 보냅니다. 프론트엔드는 이 응답을 감지하여 토큰 갱신(Refresh) 로직을 실행해야 합니다.
    *   토큰 갱신 로직: `POST /api/auth/refresh` 엔드포인트에 요청을 보냅니다. 이 요청은 브라우저가 자동으로 Refresh Token 쿠키를 함께 보낼 것입니다. 성공적으로 응답을 받으면 새로운 Access Token 쿠키가 설정됩니다.
*   **로그아웃 처리**: `/api/auth/logout` 엔드포인트를 호출하는 것만으로 충분합니다. 백엔드에서 쿠키를 무효화하고 브라우저에서 쿠키를 삭제하도록 지시할 것입니다.
*   **JavaScript에서 토큰 접근 불가**: `HttpOnly` 속성 때문에 클라이언트 측 JavaScript 코드에서 `document.cookie` 등으로 Access Token이나 Refresh Token 값에 직접 접근할 수 없습니다. 이는 보안을 위한 의도적인 제약입니다.