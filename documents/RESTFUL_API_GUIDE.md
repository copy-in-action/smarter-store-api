# RESTful API 가이드

## API 문서 (Swagger UI)

애플리케이션 실행 후 아래 링크에서 API 명세를 확인하고 테스트할 수 있습니다.

- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## HTTP 메서드별 응답 규칙

| HTTP 메서드 | 용도 | 상태 코드 | 응답 Body |
|:---:|:---|:---:|:---|
| POST | 리소스 생성 | 201 Created | 생성된 리소스 |
| GET | 리소스 조회 | 200 OK | 조회된 리소스 |
| PUT | 리소스 수정 | 200 OK | 수정된 리소스 |
| DELETE | 리소스 삭제 | 204 No Content | 없음 |

## 에러 상태 코드

| 상태 코드 | 설명 |
|:---:|:---|
| 400 Bad Request | 잘못된 입력 값 |
| 401 Unauthorized | 인증 실패 |
| 403 Forbidden | 권한 없음 |
| 404 Not Found | 리소스 없음 |
| 409 Conflict | 중복 데이터 |

## 응답 원칙

- 생성/조회/수정 시 해당 리소스 정보 반환
- timestamp 필드(createdAt, updatedAt) 포함
- 민감 정보(비밀번호 등) 제외
