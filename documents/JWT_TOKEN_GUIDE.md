# JWT 토큰 가이드

## 개요

JWT(JSON Web Token) 기반 인증 시스템으로, Access Token과 Refresh Token을 사용한 이중 토큰 방식을 적용합니다.

## 토큰 종류

| 토큰 | 유효기간 | 저장 위치 | 용도 |
|:---|:---:|:---:|:---|
| Access Token | 1시간 | 클라이언트 | API 인증 |
| Refresh Token | 7일 | DB (refresh_tokens 테이블) | Access Token 갱신 |

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

**응답**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "expiresIn": 3600
}
```

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

**요청**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**응답**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...(새 토큰)",
  "refreshToken": "새로운-uuid-값",
  "expiresIn": 3600
}
```

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

### 토큰 저장
- Access Token: 메모리 또는 sessionStorage
- Refresh Token: httpOnly 쿠키 권장 (또는 localStorage)

### 토큰 갱신 로직
```javascript
// Axios 인터셉터 예시
axios.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      const { data } = await axios.post('/api/auth/refresh', {
        refreshToken: getRefreshToken()
      });
      // 새 토큰 저장
      setTokens(data.accessToken, data.refreshToken);
      // 원래 요청 재시도
      error.config.headers.Authorization = `Bearer ${data.accessToken}`;
      return axios(error.config);
    }
    return Promise.reject(error);
  }
);
```

## 보안 고려사항

1. **JWT Secret**: 최소 256비트 이상, 환경변수로 관리
2. **HTTPS 필수**: 토큰 탈취 방지
3. **Token Rotation**: Refresh Token 재사용 공격 방지
4. **짧은 Access Token 유효기간**: 탈취 시 피해 최소화
