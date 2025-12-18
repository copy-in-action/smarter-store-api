# 프로젝트 개요

`smarter-store-api`는 **Kotlin**과 **Spring Boot 3.3.0**으로 구축된 백엔드 REST API 서버입니다. **도메인 주도 설계(DDD)** 및 **테스트 주도 개발(TDD)** 원칙을 따릅니다.

**주요 기술 스택:**
- **언어**: Kotlin
- **프레임워크**: Spring Boot (Spring Security, Spring Data JPA)
- **빌드 도구**: Gradle (Kotlin DSL)
- **데이터베이스**: PostgreSQL
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
[자세한 개발 컨벤션은 여기를 참고하세요.](documents/DEVELOPMENT_CONVENTION.md)
