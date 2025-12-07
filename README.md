# Smarter Store API

Kotlin + Spring Boot로 구축된 백엔드 REST API 서버입니다.
DDD(도메인 주도 설계)와 TDD(테스트 주도 개발) 원칙을 따릅니다.

## 기술 스택

- **언어**: Kotlin
- **프레임워크**: Spring Boot 3.3.0 (Spring Security, Spring Data JPA)
- **빌드 도구**: Gradle (Kotlin DSL)
- **데이터베이스**: PostgreSQL + Flyway
- **인증**: JWT
- **요청 검증**: Jakarta Validation
- **API 문서**: Swagger (`/swagger-ui/index.html`)
- **로깅**: SLF4J + Logback
- **테스트**: JUnit 5 + MockK

## 빌드 및 실행

```bash
./gradlew build      # 빌드
./gradlew bootRun    # 실행 (localhost:8080)
./gradlew test       # 테스트
```

## 문서

[documents/](documents/) 폴더에서 상세 가이드 확인

## 개발 이력

[CHANGELOG.md](CHANGELOG.md) 참조
