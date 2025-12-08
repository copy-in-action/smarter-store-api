# 프로젝트 개요

`smarter-store-api`는 **Kotlin**과 **Spring Boot 3.3.0**으로 구축된 백엔드 REST API 서버입니다. **도메인 주도 설계(DDD)** 및 **테스트 주도 개발(TDD)** 원칙을 따릅니다.

**주요 기술 스택:**
- **언어**: Kotlin
- **프레임워크**: Spring Boot (Spring Security, Spring Data JPA)
- **빌드 도구**: Gradle (Kotlin DSL)
- **데이터베이스**: PostgreSQL (Flyway를 이용한 마이그레이션 관리)
- **인증**: JWT (JSON Web Tokens)
- **API 문서**: Springdoc OpenAPI (Swagger UI)
- **로깅**: SLF4J + Logback
- **테스트**: JUnit 5 + MockK
- **런타임**: Java 21 (Docker에서는 Temurin 배포판 사용)

이 애플리케이션은 Spring Profiles (`local`, `prod`)와 환경 변수를 사용하여 민감한 정보에 대해 높은 구성 가능성을 갖도록 설계되었습니다.

## 빌드 및 실행

### 필수 조건

*   Java Development Kit (JDK) 21
*   Gradle (일반적으로 `./gradlew`를 통해 사용)
*   Docker Desktop (로컬 Docker 배포용)

### 1. 애플리케이션 빌드 (JAR 파일)

Spring Boot 애플리케이션의 실행 가능한 JAR 파일을 빌드하려면:

```bash
./gradlew clean build -x test
```
이 명령어는 프로젝트를 클린하고, 테스트 실행을 건너뛰고 빌드하여 `build/libs/` 디렉토리에 JAR 파일을 생성합니다.

### 2. 로컬에서 실행 (Docker 없이)

JAR 파일을 직접 실행하려면:

```bash
# JAVA_HOME이 JDK 21을 가리키도록 설정되었는지 확인
# JWT_SECRET_LOCAL 환경 변수 설정 (예: $env:JWT_SECRET_LOCAL="your_secret_key")
# application-local.yml을 사용하는 경우 DB_URL, DB_USERNAME, DB_PASSWORD 환경 변수 설정
./gradlew bootRun
```
애플리케이션은 일반적으로 `http://localhost:8080`에서 시작됩니다. Swagger UI는 `http://localhost:8080/swagger-ui/index.html`에서 접근할 수 있습니다.

### 3. Docker를 이용한 로컬 실행

Docker를 이용한 로컬 배포를 위해서는:

#### a. Docker 이미지 빌드

먼저 `Dockerfile`이 프로젝트 루트에 있는지 확인한 후 Docker 이미지를 빌드합니다:

```bash
docker build -t smarter-store-api:latest .
```

#### b. Docker 컨테이너 실행

실행하기 전에 `JWT_SECRET_LOCAL` 환경 변수가 PowerShell 세션에 설정되어 있는지 확인하세요 (실제 키 값으로 교체):

```powershell
# 1. JWT_SECRET_LOCAL 환경 변수 설정 (이전에 생성한 Base64 키로 대체하세요)
$env:JWT_SECRET_LOCAL="your_local_base64_secret_here"

# 2. Docker 컨테이너 실행 명령어 (PowerShell용)
# 다음 명령어를 통째로 복사해서 PowerShell에 붙여넣고 엔터키를 누르세요.
docker run -d `
  -p 8080:8080 `
  --name smarter-store-api-local `
  -v ./logs:/app/logs `
  -e SPRING_PROFILES_ACTIVE=local `
  -e JWT_SECRET_LOCAL="$env:JWT_SECRET_LOCAL" `
  smarter-store-api:latest
```
`http://localhost:8080/swagger-ui/index.html`에서 Swagger UI에 접근하세요.

#### c. Docker Compose로 실행 (단일 또는 다중 서비스 설정)

`docker-compose.yml` 파일이 있다면 (예: 데이터베이스 통합을 위해), 쉘에 `JWT_SECRET_LOCAL`이 설정되어 있는지 확인한 후 다음을 실행합니다:

```bash
docker-compose up -d
```
중지하려면 `docker-compose down`을 사용합니다.

### 4. 테스트

모든 테스트를 실행하려면:

```bash
./gradlew test
```

## 개발 컨벤션

*   **언어 및 플랫폼**: Kotlin, Spring Boot 기반, Java 21 대상.
*   **빌드 시스템**: Kotlin DSL을 사용하는 Gradle.
*   **의존성**: `build.gradle.kts`를 통해 관리되며, 일반적인 기능(web, data-jpa, security, validation)을 위한 Spring Boot starter 사용.
*   **API 문서**: `springdoc-openapi-starter-webmvc-ui`를 사용하여 Swagger UI 생성.
*   **보안**: JWT를 사용한 Spring Security 인증. JWT Secret은 환경 변수를 통해 관리되며 Base64 인코딩. Swagger UI 엔드포인트(`swagger-ui/**`, `v3/api-docs/**`)는 공개 접근을 허용하도록 설정.
*   **데이터베이스**: PostgreSQL, Flyway를 사용한 데이터베이스 마이그레이션. 데이터베이스 연결 정보는 환경 변수로 외부화.
*   **로깅**: `logback-spring.xml`을 통해 구성되며, 로그는 `logs` 디렉토리에 기록.
*   **설정**: Spring Profiles (`local`, `prod`) 및 환경 변수 (`${VAR_NAME}`)를 활용하여 민감한 데이터 및 환경별 설정 관리. 로컬 개발에서 `.env` 파일 지원을 위해 `spring-dotenv` 사용.
*   **Dockerfile**: `eclipse-temurin:21-jdk`를 베이스 이미지로 사용하며, `WORKDIR /app`, 8080 포트 노출, 로그용 볼륨 설정 및 Entrypoint 정의.
*   **Git Hooks**: `.gitattributes` 및 `.gitignore`를 버전 관리용으로 사용.

## 주요 파일

*   **`build.gradle.kts`**: 메인 Gradle 빌드 스크립트, 의존성, 플러그인, Java 버전 및 Flyway 구성 정의.
*   **`settings.gradle.kts`**: 루트 프로젝트 이름을 정의.
*   **`src/main/kotlin/com/github/copyinaction/SmarterStoreApiApplication.kt`**: Spring Boot 애플리케이션의 메인 진입점.
*   **`src/main/resources/application.yml`**: 기본 애플리케이션 구성, 기본 프로필, JWT 기본값, Actuator 설정.
*   **`src/main/resources/application-local.yml`**: 로컬 개발 프로필별 구성, 데이터소스, 로깅 경로, JWT secret 플레이스홀더 (`JWT_SECRET_LOCAL`), CORS Origin 포함.
*   **`src/main/resources/application-prod.yml`**: 프로덕션 프로필별 구성, 안전한 데이터소스 자격 증명 (`PROD_DB_URL`, `PROD_DB_USERNAME`, `PROD_DB_PASSWORD`) 및 JWT secret (`JWT_SECRET_PROD`)을 위한 환경 변수 플레이스홀더 포함.
*   **`Dockerfile`**: 베이스 이미지(`eclipse-temurin:21-jdk`), JAR 복사, 작업 디렉토리, 볼륨 및 진입점 정의를 포함한 Docker 이미지 빌드 프로세스 정의.
*   **`docker-compose.yml`**: 로컬 배포용 Docker 서비스를 정의하며, 현재 `smarter-store-api` 서비스만 실행 (외부 DB에 연결).
*   **`src/main/kotlin/com/github/copyinaction/config/jwt/JwtTokenProvider.kt`**: JWT 토큰 생성 및 검증 처리, Base64 시크릿 키 디코딩 포함.
*   **`src/main/kotlin/com/github/copyinaction/config/SecurityConfig.kt`**: Spring Security 구성, 요청 매처, 세션 관리, 비밀번호 인코더 정의 및 JWT 인증 필터 통합. `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`에 대한 `permitAll()` 허용.
*   **`documents/Docker_로컬_배포_가이드.md`**: Dockerfile 설정, 빌드, 실행 및 Docker Compose 사용법을 포함한 로컬 Docker 배포를 위한 자세한 단계별 가이드.
*   **`CHANGELOG.md`**: 프로젝트 개발 이력 및 주요 변경 사항.
