# CI/CD 배포 설계 가이드 (GitHub Actions & Docker)

## 개정이력

| 버전 | 일자 | 작성자 | 내용 |
|------|------|--------|------|
| 1.0 | 2025-12-29 | Gemini | 초기 배포 설계 문서화 |

---

이 가이드는 GitHub Actions와 Docker를 사용하여 Spring Boot 애플리케이션을 원격 서버에 자동 배포하는 방법을 설명합니다.

## 1. 배포 아키텍처

1. **코드 푸시**: `main` 브랜치에 푸시 발생.
2. **CI (GitHub Actions)**: Gradle 빌드 및 테스트 수행.
3. **Docker 빌드 (Jib)**: Docker daemon 없이 이미지를 빌드하여 GHCR(GitHub Container Registry)에 푸시.
4. **CD (SSH)**: 원격 서버 접속 후 최신 이미지를 Pull 하여 컨테이너 재실행.

## 2. 주요 설정

### 2.1. Jib 설정 (`build.gradle.kts`)
- 베이스 이미지: `eclipse-temurin:21-jre-jammy`
- 대상 레지스트리: `ghcr.io`

### 2.2. GitHub Secrets
- `SSH_PRIVATE_KEY`: 서버 접속용 키
- `SERVER_IP`, `SERVER_USER`: 서버 정보
- `JWT_SECRET_PROD`: 운영 서버용 JWT 비밀키

## 3. 서버 관리 (Systemd)
서버에서는 컨테이너를 `systemd` 서비스로 등록하여 장애 발생 시 자동 재시작되도록 관리하는 것을 권장합니다.