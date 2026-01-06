# Smarter Store API Project

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.1  | 2026-01-06 | Gemini | 전역 컨벤션 통합 및 문서 구조 최적화 |
| 1.0  | 2025-12-18 | Gemini | 초기 버전 작성 |

`smarter-store-api`는 **Kotlin**과 **Spring Boot 3.3.0**으로 구축된 백엔드 REST API 서버입니다. **도메인 주도 설계(DDD)** 및 **테스트 주도 개발(TDD)** 원칙을 엄격히 준수합니다.

---

## 🛠 주요 기술 스택

- **언어 및 런타임**: Kotlin (JVM 21 / Temurin)
- **프레임워크**: Spring Boot 3.3.0 (Security, Data JPA)
- **데이터베이스**: PostgreSQL
- **인증/보안**: JWT (JSON Web Tokens)
- **문서화**: Springdoc OpenAPI (Swagger UI)
- **로깅**: SLF4J + Logback
- **테스팅**: JUnit 5 + MockK
- **빌드 도구**: Gradle (Kotlin DSL)

---

## 🚀 시작 가이드

### 필수 조건
* JDK 21
* Gradle (또는 `./gradlew`)
* Docker Desktop (로컬 배포용)

### 1. 애플리케이션 빌드
```bash
./gradlew clean build -x test
```

### 2. 로컬 실행 (bootRun)
```bash
# 환경 변수 설정 필요: JWT_SECRET_LOCAL, DB_URL, DB_USERNAME, DB_PASSWORD
./gradlew bootRun
```

### 3. Docker를 이용한 실행
```bash
# 이미지 빌드
docker build -t smarter-store-api:latest .

# 컨테이너 실행 (PowerShell 예시)
$env:JWT_SECRET_LOCAL="your_secret_key"
docker run -d -p 8080:8080 --name smarter-store-api-local `
  -e SPRING_PROFILES_ACTIVE=local `
  -e JWT_SECRET_LOCAL="$env:JWT_SECRET_LOCAL" `
  smarter-store-api:latest
```

---

## 🏗 개발 표준 및 원칙

상세 내용은 [개발 컨벤션 가이드](documents/DEVELOPMENT_CONVENTION.md)를 참조하세요.

### 핵심 요약
- **아키텍처**: DDD 원칙 준수. 도메인 엔티티에 비즈니스 로직을 응집시키고, 서비스는 이를 조율합니다.
- **코드 스타일**:
    - Kotlin Idiomatic 스타일 지향.
    - 클래스/인터페이스: `PascalCase`, 변수/함수: `camelCase`, 상수: `SCREAMING_SNAKE_CASE`.
    - 들여쓰기: 4칸 공백. 주석은 '무엇'보다 **'왜'**에 집중.
- **패키지 구조**: 도메인별 계층화 (`domain`, `repository`, `service`, `controller`, `dto`).
- **DDD 구현**: Repository는 **Aggregate Root** 단위로만 생성 (예: `PaymentItemRepository` 생성 지양).
- **데이터 변환**: DTO의 `toEntity()` 대신 엔티티의 **팩토리 메서드**(예: `Venue.create()`) 사용.
- **테스팅**: TDD 접근 방식. 별도 요청 전까지 **Domain 단위 테스트** 작성을 원칙으로 함.
- **API 설계**: RESTful 준수. Orval 연동을 위해 `@Tag` name은 **소문자 케밥 케이스**(예: `venue-management`) 사용.

---

## 🤖 Gemini 에이전트 협업 규칙

에이전트와의 원활한 협업을 위한 가이드라인입니다.

1. **페르소나**: 시니어 백엔드 개발자로서 꼼꼼한 코드 리뷰와 기술적 조언을 제공합니다.
2. **언어**: 모든 답변은 한국어로 상세하게 제공합니다.
3. **Jira 티켓**: 티켓 번호는 항상 대괄호(`[]`)로 감싸 표기합니다 (예: `[CCS-125]`).
4. **변경 이력**: `CHANGELOG.md` 업데이트 시, 동일 날짜의 변경 사항은 기존 날짜 섹션 하위에 통합합니다.
5. **보안**: 비밀키나 민감 정보가 코드에 노출되지 않도록 엄격히 관리합니다.