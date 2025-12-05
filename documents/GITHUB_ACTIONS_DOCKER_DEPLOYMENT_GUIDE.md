# GitHub Actions를 활용한 Docker 기반 서버 배포 가이드

이 가이드는 GitHub Actions와 Docker를 사용하여 Spring Boot 애플리케이션을 원격 서버에 배포하는 방법을 단계별로 설명합니다. Docker 기반 배포는 애플리케이션의 일관성, 이식성 및 확장성을 크게 향상시킬 수 있습니다.

## 1. 소개

Docker는 애플리케이션과 모든 의존성을 컨테이너라는 표준화된 단위로 패키징하는 기술입니다. GitHub Actions와 결합하면, 코드 변경 시 자동으로 Docker 이미지를 빌드하고, 이를 컨테이너 레지스트리에 푸시한 다음, 원격 서버에서 해당 이미지를 가져와 실행하는 CI/CD 파이프라인을 구축할 수 있습니다.

**장점:**
*   **환경 일관성:** 개발, 테스트, 운영 환경 간의 불일치 문제를 해결하여 "내 컴퓨터에서는 되는데..." 문제를 줄입니다.
*   **쉬운 확장성:** 컨테이너화된 애플리케이션은 필요에 따라 쉽게 확장하거나 축소할 수 있습니다.
*   **빠른 배포:** 이미지를 빌드한 후에는 서버에서 이미지를 풀(pull)하는 것만으로 배포가 가능하여 배포 시간을 단축할 수 있습니다.
*   **이식성:** Docker가 설치된 어떤 환경에서든 동일하게 실행됩니다.

## 2. 사전 준비 사항

Docker 기반 배포를 시작하기 전에 다음 사항들을 준비해야 합니다.

*   **GitHub 저장소**: 배포할 코드가 있는 GitHub 저장소.
*   **원격 서버**: Docker가 설치된 Linux 기반의 원격 서버 (예: AWS EC2, Google Cloud VM 등).
    *   서버에 Docker가 설치되어 있지 않다면, 먼저 Docker를 설치해야 합니다. ([Docker 설치 가이드](https://docs.docker.com/engine/install/) 참조)
*   **Docker Hub 또는 GitHub Container Registry (GHCR) 계정**: 빌드된 Docker 이미지를 저장할 컨테이너 레지스트리 계정이 필요합니다. GitHub Actions를 사용한다면 GHCR 사용을 권장합니다.
*   **SSH 접근**: 원격 서버에 SSH로 접속할 수 있는 권한 및 정보 (IP 주소, 사용자명, SSH 키).
*   **Java/Gradle 환경**: GitHub Actions 러너에서 애플리케이션 빌드를 위한 JRE/JDK가 필요합니다.

## 3. 핵심 개념

*   **Dockerfile**: Docker 이미지를 빌드하기 위한 지침이 담긴 텍스트 파일입니다.
*   **Docker Image**: 애플리케이션 실행에 필요한 모든 것(코드, 런타임, 시스템 도구, 라이브러리, 설정)을 포함하는 실행 가능한 패키지입니다.
*   **Docker Container**: Docker 이미지의 실행 가능한 인스턴스입니다.
*   **Docker Registry**: Docker 이미지를 저장하고 공유하는 서비스 (예: Docker Hub, GHCR).

## 4. 배포 전략 (Docker 기반)

1.  **Dockerfile 작성**: Spring Boot 애플리케이션을 위한 `Dockerfile`을 작성합니다.
2.  **Docker 이미지 빌드**: GitHub Actions 워크플로우에서 `Dockerfile`을 사용하여 Docker 이미지를 빌드합니다. (Jib Gradle 플러그인 사용)
3.  **레지스트리 푸시**: 빌드된 이미지를 GHCR과 같은 컨테이너 레지스트리에 푸시합니다.
4.  **서버 배포**: SSH를 통해 원격 서버에 접속하여, 레지스트리에서 최신 Docker 이미지를 풀(pull)하고 기존 컨테이너를 중지/제거 후 새 이미지로 컨테이너를 실행합니다.
5.  **`systemd` 서비스 관리**: 서버에서 `systemd`를 사용하여 Docker 컨테이너를 서비스로 관리합니다.

## 5. 단계별 가이드

### 5.1 Dockerfile 작성

프로젝트 루트 디렉토리에 `Dockerfile`을 생성하고 다음 내용을 추가합니다. (기본적인 Spring Boot 애플리케이션용)

```dockerfile
# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-jammy as builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src

# Make gradlew executable
RUN chmod +x ./gradlew

# Build the Spring Boot application
RUN ./gradlew bootJar

# Stage 2: Create the final Docker image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copy the built JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar
# Expose the port your Spring Boot application runs on (default 8080)
EXPOSE 8080
# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 5.2 `build.gradle.kts` 수정 (Jib 플러그인 사용)

Gradle의 Jib 플러그인은 Docker daemon 없이도 Docker 이미지를 빌드하고 레지스트리에 푸시할 수 있게 해주는 도구입니다.

`build.gradle.kts` 파일에 다음 내용을 추가하거나 수정합니다.

```kotlin
// ... (plugins block)
plugins {
    // ... other plugins
    id("com.google.cloud.tools.jib") version "3.4.1" // Jib 플러그인 추가
}

// ... (repositories, dependencies blocks)

// Jib 플러그인 설정
jib {
    from {
        image = "eclipse-temurin:21-jre-jammy" // 베이스 이미지 (Dockerfile의 Stage 2와 동일)
    }
    to {
        // GHCR (GitHub Container Registry) 사용 예시
        image = "ghcr.io/copy-in-action/smarter-store-api" // [YOUR_GITHUB_USERNAME]/[YOUR_REPO_NAME] 또는 [YOUR_ORG_NAME]/[YOUR_REPO_NAME]
        tags = ["${project.version}", "latest"] // 이미지 태그
        auth {
            username = System.getenv("DOCKER_USERNAME") // GitHub Actions Secrets에서 가져옴
            password = System.getenv("DOCKER_PASSWORD") // GitHub Actions Secrets에서 가져옴 (GHCR은 GITHUB_TOKEN 사용 가능)
        }
    }
    container {
        mainClass = "com.github.copyinaction.SmarterStoreApiApplicationKt" // 메인 클래스 경로
        ports = listOf("8080") // 애플리케이션 포트
        jvmFlags = listOf("-Xms512m", "-Xmx1024m") // JVM 메모리 설정
        args = listOf("--spring.profiles.active=prod") // 애플리케이션 실행 인자 (prod 프로파일 활성화)
    }
}
```
*   `image = "ghcr.io/copy-in-action/smarter-store-api"`: 이 부분을 **사용자님의 GitHub 사용자명/조직명과 저장소 이름에 맞게 수정**해야 합니다.
*   GHCR을 사용할 경우, `DOCKER_USERNAME`은 `GITHUB_ACTOR`로, `DOCKER_PASSWORD`는 내장 `GITHUB_TOKEN`으로 설정할 수 있습니다. (아래 5.3 GitHub Secrets 설정 참조)

### 5.3 GitHub Secrets 설정

GitHub Actions 러너가 Docker 레지스트리에 이미지를 푸시하려면 인증 정보가 필요합니다. 또한 서버 접속 정보도 필요합니다.

1.  **GitHub Container Registry (GHCR) 사용 시:**
    *   **Name**: `DOCKER_USERNAME`, **Value**: `GITHUB_ACTOR` (GitHub Actions에서 자동으로 제공되는 환경 변수, 사용자명)
    *   **Name**: `DOCKER_PASSWORD`, **Value**: `GITHUB_TOKEN` (GitHub Actions에서 자동으로 제공되는 내장 토큰)
    *   `GITHUB_TOKEN`은 기본적으로 패키지 쓰기 권한을 포함하므로 GHCR 인증에 사용될 수 있습니다.

2.  **SSH 개인 키 및 서버 접속 정보:**
    *   `SSH_PRIVATE_KEY`: 원격 서버 접속을 위한 개인 키 (기존 설정과 동일).
    *   `SERVER_IP`: 원격 서버의 IP 주소 또는 도메인 (기존 설정과 동일).
    *   `SERVER_USER`: 원격 서버 접속 사용자 이름 (기존 설정과 동일).
    *   `SERVER_APP_PATH`: 서버에서 Docker Compose 파일 등이 위치할 경로 (예: `/home/cic/deploy`). (기존 JAR 경로와 다르게 설정하는 것이 좋음)

### 5.4 GitHub Actions 워크플로우 (`.github/workflows/deploy-docker.yml`) 수정

기존 `deploy.yml` 대신 `deploy-docker.yml` 파일을 새로 생성하거나 수정합니다.

```yaml
# .github/workflows/deploy-docker.yml
name: Deploy Spring Boot Application with Docker

on:
  push:
    branches:
      - main # main 브랜치에 푸시될 때 워크플로우 실행

env:
  JAVA_VERSION: '21'
  DOCKER_IMAGE_NAME: ghcr.io/copy-in-action/smarter-store-api # [YOUR_GITHUB_USERNAME]/[YOUR_REPO_NAME] 또는 [YOUR_ORG_NAME]/[YOUR_REPO_NAME]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest

    permissions:
      contents: read
      packages: write # GHCR에 푸시하기 위해 필요

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: ${{ env.JAVA_VERSION }}

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Login to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }} # GHCR에 푸시할 때 사용하는 사용자명
          password: ${{ secrets.GITHUB_TOKEN }} # 내장 GITHUB_TOKEN 사용

      - name: Build and Push Docker Image with Jib
        run: |
          ./gradlew jibBuild \
            -Djib.to.image=${{ env.DOCKER_IMAGE_NAME }} \
            -Djib.to.tags=${{ github.sha }},latest \
            -Djib.to.auth.username=${{ github.actor }} \
            -Djib.to.auth.password=${{ secrets.GITHUB_TOKEN }}

      - name: Create SSH key file
        run: |
          mkdir -p ~/.ssh
          echo "${{ secrets.SSH_PRIVATE_KEY }}" > ~/.ssh/id_rsa
          chmod 600 ~/.ssh/id_rsa
          ssh-keyscan -H ${{ secrets.SERVER_IP }} >> ~/.ssh/known_hosts

      - name: Deploy to Server via Docker
        run: |
          ssh -i ~/.ssh/id_rsa -p 30022 ${{ secrets.SERVER_USER }}@${{ secrets.SERVER_IP }} << EOF
            # 배포 경로로 이동 (예: Docker Compose 파일이 있을 경로)
            mkdir -p ${{ secrets.SERVER_APP_PATH }}
            cd ${{ secrets.SERVER_APP_PATH }}

            # .env 파일 생성 또는 업데이트 (필요한 환경 변수)
            # echo "SPRING_PROFILES_ACTIVE=prod" > .env
            # echo "DB_URL=jdbc:postgresql://localhost:5432/smarter_store" >> .env
            # echo "DB_USERNAME=postgres" >> .env
            # echo "DB_PASSWORD=sStore1@3" >> .env
            # echo "JWT_SECRET=${{ secrets.JWT_SECRET_PROD }}" >> .env # GitHub Secrets에서 JWT 비밀키 가져오기

            # 최신 Docker 이미지 풀
            docker pull ${{ env.DOCKER_IMAGE_NAME }}:latest

            # 기존 컨테이너 중지 및 제거
            docker stop smarter-store-api || true
            docker rm smarter-store-api || true

            # 새 이미지로 컨테이너 실행
            docker run -d --name smarter-store-api \
              -p 8080:8080 \
              --restart always \
              -e SPRING_PROFILES_ACTIVE=prod \
              -e DB_URL="jdbc:postgresql://localhost:5432/smarter_store" \
              -e DB_USERNAME="postgres" \
              -e DB_PASSWORD="sStore1@3" \
              -e JWT_SECRET="${{ secrets.JWT_SECRET_PROD }}" \ # Secrets에 등록된 운영용 JWT 비밀키
              ${{ env.DOCKER_IMAGE_NAME }}:latest
            
            # Docker 컨테이너 상태 확인
            docker ps -f name=smarter-store-api
EOF
```
*   `DOCKER_IMAGE_NAME`: 이 환경 변수의 `ghcr.io/copy-in-action/smarter-store-api` 부분을 **사용자님의 GitHub 사용자명/조직명과 저장소 이름에 맞게 수정**해야 합니다.
*   Docker `run` 명령어의 `-e` 옵션으로 전달되는 환경 변수들은 실제 운영 환경에 맞게 GitHub Secrets에서 가져오거나 `.env` 파일 등을 통해 관리하는 것이 좋습니다. 여기서는 예시로 직접 명시했습니다. `JWT_SECRET_PROD`와 같은 Secrets도 필요에 따라 GitHub에 추가해야 합니다.

### 5.5 서버 `systemd` 서비스 수정 (선택 사항)

Docker 컨테이너를 `systemd`로 관리하려면 서비스 파일 내용이 달라집니다. (위 GitHub Actions 워크플로우 예시에서는 `ssh` 스크립트 내에서 `docker run`을 직접 실행하므로 `systemd` 서비스 파일이 반드시 필요한 것은 아니지만, 컨테이너 재시작 및 관리를 위해 사용하는 것을 권장합니다.)

**파일 경로:** `/etc/systemd/system/smarter-store-api-docker.service` (기존 파일명과 구분)

```ini
[Unit]
Description=Smarter Store API Docker Service
After=docker.service
Requires=docker.service

[Service]
ExecStartPre=-/usr/bin/docker stop smarter-store-api
ExecStartPre=-/usr/bin/docker rm smarter-store-api
ExecStart=/usr/bin/docker run --name smarter-store-api \
          -p 8080:8080 \
          --restart always \
          -e SPRING_PROFILES_ACTIVE=prod \
          -e DB_URL="jdbc:postgresql://localhost:5432/smarter_store" \
          -e DB_USERNAME="postgres" \
          -e DB_PASSWORD="sStore1@3" \
          -e JWT_SECRET="your_prod_jwt_secret" \ # 이 값은 GitHub Secrets에서 가져와야 함 (수동으로 채워 넣는다면)
          ghcr.io/copy-in-action/smarter-store-api:latest # [YOUR_GITHUB_USERNAME]/[YOUR_REPO_NAME]
ExecStop=/usr/bin/docker stop smarter-store-api
ExecStopPost=/usr/bin/docker rm smarter-store-api
User=cic # Docker 명령을 실행할 권한이 있는 사용자
Restart=on-failure
RestartSec=5s

[Install]
WantedBy=multi-user.target
```
*   이 `systemd` 서비스 파일을 사용하려면, 워크플로우의 `ssh` 스크립트에서 `docker run` 대신 `sudo systemctl start smarter-store-api-docker.service`와 같은 명령어를 실행해야 합니다.
*   `JWT_SECRET`과 같은 민감 정보는 `systemd` 파일에 직접 넣는 대신, `/etc/environment`나 `docker run`의 `--env-file` 옵션 등을 통해 관리하는 것이 더 안전합니다.

## 6. 모범 사례

*   **Secrets 관리**: 민감한 정보(Docker 인증 정보, JWT 비밀키 등)는 GitHub Secrets에 저장하고 환경 변수로 주입합니다.
*   **`.dockerignore` 파일**: `Dockerfile`과 같은 디렉토리에 `.dockerignore` 파일을 생성하여 불필요한 파일(예: `build`, `.git`, `.gradle`)이 Docker 이미지에 포함되지 않도록 합니다.
*   **이미지 태깅**: `latest` 태그 외에 `git commit SHA`나 버전 번호를 태그로 사용하여 이미지 버전을 명확히 관리합니다.
*   **Health Check**: Docker 컨테이너에 Health Check를 구성하여 컨테이너가 정상적으로 작동하는지 모니터링합니다.

이 가이드가 Docker 기반 배포 전략을 수립하는 데 도움이 되기를 바랍니다.
