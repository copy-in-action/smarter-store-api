# JWT 토큰 가이드

## 개요

JWT(JSON Web Token) 기반 인증 시스템으로, Access Token과 Refresh Token을 사용한 이중 토큰 방식을 적용합니다.

## 토큰 종류

| 토큰 | 유효기간 | 저장 위치 | 용도 |
|:---|:---:|:---:|:---|
| Access Token | 1시간 | HttpOnly 쿠키 | API 인증 |
| Refresh Token | 7일 | HttpOnly 쿠키 | Access Token 갱신 |

## 설정

### application.yml
```yaml
jwt:
  secret: ${JWT_SECRET}  # Base64 인코딩된 256비트 이상 키
  access-token-validity-in-seconds: 3600    # 1시간
  refresh-token-validity-in-seconds: 604800 # 7일
```

## 인증 흐름

### 1. 로그인
```
POST /api/auth/login
```

**요청**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**응답 (HTTP Headers)**
```
HTTP/1.1 200 OK
Set-Cookie: accessToken=<your_access_token>; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=3600
Set-Cookie: refreshToken=<your_refresh_token>; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=604800
```
(응답 본문은 비어 있습니다 - `200 OK`)

### 2. API 요청
```
GET /api/products
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 3. 토큰 갱신
Access Token 만료 시 Refresh Token으로 새 토큰 발급

```
POST /api/auth/refresh
```

**요청 (브라우저가 자동으로 Refresh Token 쿠키 전송)**
```
POST /api/auth/refresh
```

**응답 (HTTP Headers)**
```
HTTP/1.1 200 OK
Set-Cookie: accessToken=<new_access_token>; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=3600
Set-Cookie: refreshToken=<new_refresh_token>; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=604800
```
(응답 본문은 비어 있습니다 - `200 OK`)

## Access Token 구조

### Header
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

### Payload
```json
{
  "sub": "user@example.com",
  "auth": "ROLE_USER",
  "iat": 1701849600,
  "exp": 1701853200
}
```

| 필드 | 설명 |
|:---|:---|
| sub | 사용자 이메일 (Subject) |
| auth | 권한 (ROLE_USER, ROLE_ADMIN) |
| iat | 발급 시간 (Issued At) |
| exp | 만료 시간 (Expiration) |

## Refresh Token 특징

- **UUID 기반**: JWT가 아닌 UUID 문자열 사용
- **DB 저장**: refresh_tokens 테이블에 저장
- **Token Rotation**: 갱신 시 기존 토큰 삭제 후 새 토큰 발급 (보안 강화)
- **사용자당 1개**: 로그인 시 기존 Refresh Token 삭제

## 에러 응답

| 상황 | 에러 코드 | HTTP 상태 |
|:---|:---|:---:|
| Access Token 만료/유효하지 않음 | - | 401 |
| Refresh Token 없음/유효하지 않음 | INVALID_REFRESH_TOKEN | 401 |
| Refresh Token 만료 | EXPIRED_REFRESH_TOKEN | 401 |

## 관련 파일

| 파일 | 설명 |
|:---|:---|
| `config/jwt/JwtTokenProvider.kt` | 토큰 생성, 검증, 파싱 |
| `config/jwt/JwtAuthenticationFilter.kt` | 요청 헤더에서 토큰 추출 및 인증 |
| `domain/RefreshToken.kt` | Refresh Token 엔티티 |
| `repository/RefreshTokenRepository.kt` | Refresh Token Repository |
| `service/AuthService.kt` | 로그인, 토큰 갱신 로직 |

## 클라이언트 구현 가이드

주요 변경 사항은 다음과 같습니다:
*   토큰을 `localStorage`에 저장하지 않습니다.
*   브라우저가 HTTP 요청 시 쿠키를 자동으로 전송하므로, 프론트엔드 코드에서 `Authorization` 헤더를 수동으로 설정할 필요가 없습니다.
*   API 호출 시 `credentials: 'include'` (Fetch API) 또는 `withCredentials: true` (Axios) 설정을 반드시 포함해야 합니다.
*   Access Token 만료 시 `401 Unauthorized` 응답을 감지하여 `/api/auth/refresh` 엔드포인트로 토큰 갱신 요청을 보내야 합니다. (이때 브라우저는 Refresh Token 쿠키를 자동으로 보냅니다.)

## 관리자 계정의 Refresh Token 미사용

관리자 계정은 시스템에 대한 높은 권한을 가지므로 보안을 극대화하기 위해 Refresh Token을 사용하지 않을 수 있습니다.

*   **보안 강화 (공격 표면 감소):** Refresh Token은 장기간 유효하므로 탈취 시 위험이 큽니다. 관리자 계정에서 이를 사용하지 않으면 Access Token 만료 후에는 반드시 재인증을 해야 하므로, 토큰 탈취 시 공격자가 세션을 유지할 수 있는 "기회의 창"을 최소화합니다.
*   **사용자 경험 vs. 보안 트레이드오프:** 일반 사용자는 Refresh Token을 통해 편의성을 높이지만, 관리자 계정은 보안을 최우선으로 하여 더 자주 재인증을 요구하는 것이 일반적인 보안 관례입니다.
*   **감사 및 책임성 증대:** 관리자의 모든 세션은 Access Token의 짧은 만료 기간에 맞춰 모니터링되고 재인증되므로 감사 추적을 강화할 수 있습니다.

## 보안 고려사항

1. **JWT Secret**: 최소 256비트 이상, 환경변수로 관리
2. **HTTPS 필수**: 토큰 탈취 방지
3. **Token Rotation**: Refresh Token 재사용 공격 방지
4. **짧은 Access Token 유효기간**: 탈취 시 피해 최소화
