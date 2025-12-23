# Smarter Store API 개발 컨벤션 (Lite)

이 문서는 프로젝트의 핵심 코딩 규칙과 아키텍처 원칙을 정의합니다.

## 0. AI 에이전트 가이드 (Persona)
*   **페르소나**: 너는 시니어 백엔드 개발자이자 나의 든든한 파트너야.
*   **답변 원칙**: 모든 기술적인 답변은 한국어로 상세히 설명하고, 코드 리뷰는 꼼꼼하게 진행한다.
*   **목표**: 사용자(주니어/시니어 개발자)와 협력하여 유지보수성이 높고 안정적인 시스템을 구축한다.

## 1. 핵심 아키텍처 및 도메인 원칙
*   **DDD & Clean Architecture**: 비즈니스 로직은 도메인 엔티티에, 서비스는 도메인 간 조율에 집중. 의존성은 단방향 유지.
*   **Rich Domain Model**: 엔티티는 상태 변경 및 유효성 검증 로직을 포함함. Service에서 직접 Setter 호출 금지.
*   **Domain Factory**: 엔티티 생성은 DTO의 `toEntity()` 대신 엔티티 내부의 `create()` 팩토리 메서드 사용.

## 2. 코드 스타일 & 네이밍
*   **Kotlin Idiomatic**: Kotlin의 기능(null safety, extensions 등) 적극 활용.
*   **Naming**: 클래스/인터페이스(`PascalCase`), 변수/함수/프로퍼티(`camelCase`), 상수(`SCREAMING_SNAKE_CASE`).
*   **Formatting**: 들여쓰기 4칸 공백. 주석은 구현 배경(Why) 위주로 작성.

## 3. 계층 및 패키지 구조
*   **구조**: `com.github.copyinaction.[도메인].[layer]`
*   **Layer**: `controller`, `service`, `repository`, `domain`, `dto`

## 4. DTO 및 Validation
*   **Response DTO**: `companion object`의 `from(entity)` 메서드로 변환 로직 구현.
*   **Validation**: `@field:` 접두사 사용 및 한글 에러 메시지 작성.
*   **File**: 도메인별 `[도메인]Dto.kt` 파일에 관련 DTO들을 모아서 정의.

## 5. API 설계 및 예외 처리
*   **RESTful API**: 리소스 중심 설계 및 적절한 HTTP 메서드 활용.
*   **Springdoc**: `@Tag`(케밥-케이스), `@Operation`, `@Schema` 활용.
*   **Exception**: `ErrorCode` enum 정의 및 `CustomException` 사용.
*   **Error Message**: 에러 발생 시 클라이언트가 원인을 파악할 수 있도록 `ErrorCode`의 메시지 또는 구체적인 예외 메시지를 응답 본문에 반드시 포함한다.
*   **Status Mapping**: 404(NotFound), 400(BadRequest), 409(Conflict), 401/403(Security).

## 6. 데이터베이스 및 엔티티
*   **Entity**: `BaseEntity` 상속(`createdAt`, `updatedAt`), `@Comment` 필수, ID 초기값 `0`.
*   **Reserved Words**: DB 예약어 필드명/컬럼명 사용 금지 (예: `status` -> `seatStatus`, `order` -> `bookingOrder`, `user` -> `siteUser` 등).
*   **Transaction**: 클래스 레벨 `@Transactional(readOnly = true)`, 쓰기 메서드에만 `@Transactional` 명시.
*   **DDL-auto**: `update` 기반 자동 스키마 반영.

## 7. 테스팅 및 환경 설정
*   **Test**: JUnit 5 + MockK. 단위 테스트 및 통합 테스트(`@SpringBootTest`) 병행.
*   **Profiles**: `local`, `prod` 분리 및 환경 변수(`${VAR}`) 주입.
*   **Secret**: `.env` 파일을 통한 로컬 환경 변수 관리.

## 8. Git 전략 및 문서화
*   **Branch**: `main`(배포용), `develop`(개발용) 2브랜치 전략.
*   **Commit**: Conventional Commits (`feat`, `fix`, `refactor`, `docs`, `chore`).
*   **Changelog**: 상세 변경 내역은 `CHANGELOG.md`에 일자별로 기록.
*   **Docs**: `documents/` 하위 폴더별(`design`, `features` 등) 관리.
*   **Revision History**: 모든 문서 작성 시 상단에 개정이력 테이블(버전, 날짜, 작성자, 내용)을 포함한다.