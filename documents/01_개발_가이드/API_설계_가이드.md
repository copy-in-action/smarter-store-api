# RESTful API 설계 가이드

## 개정이력

| 버전 | 일자 | 작성자 | 내용 |
|------|------|--------|------|
| 1.0 | 2025-12-29 | Gemini | 초기 API 설계 규칙 수립 |

---

## 1. API 문서 (Swagger UI)

애플리케이션 실행 후 아래 링크에서 API 명세를 확인하고 테스트할 수 있습니다.

- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## 2. HTTP 메서드 및 응답 규칙

| 메서드 | 용도 | 상태 코드 | 응답 본문 |
|:---:|:---|:---:|:---|
| POST | 리소스 생성 | 201 Created | 생성된 데이터 |
| GET | 리소스 조회 | 200 OK | 조회된 데이터 |
| PUT | 리소스 수정 | 200 OK | 수정된 데이터 |
| DELETE | 리소스 삭제 | 204 No Content | 없음 |

## 3. 에러 처리

| 상태 코드 | 설명 |
|:---:|:---|
| 400 Bad Request | 잘못된 파라미터 또는 입력 값 |
| 401 Unauthorized | 인증 정보 부족 또는 실패 |
| 403 Forbidden | 접근 권한 부족 |
| 404 Not Found | 리소스를 찾을 수 없음 |
| 409 Conflict | 비즈니스 로직상 충돌 (중복 등) |

## 4. 응답 원칙

- 모든 성공 응답에는 해당 리소스 정보가 포함되어야 합니다.
- 생성/수정 시간 필드(`createdAt`, `updatedAt`)를 필수로 포함합니다.
- 비밀번호 등 보안상 민감한 정보는 응답에서 반드시 제외합니다.