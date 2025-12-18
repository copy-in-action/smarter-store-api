# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 필수 참조 문서

코드 작성 전 반드시 아래 문서를 확인하세요:
- **[개발 컨벤션 가이드](documents/DEVELOPMENT_CONVENTION.md)**: DDD 원칙, 코드 스타일, API 설계, DTO-Domain 팩토리 패턴 등

## 빠른 참조

```bash
./gradlew build      # 빌드
./gradlew bootRun    # 실행 (localhost:8080)
./gradlew test       # 테스트
./gradlew test --tests "패키지.클래스명.메서드명"  # 단일 테스트
```

## 프로젝트 구조

Kotlin + Spring Boot 3.3.0 기반 DDD 아키텍처. 상세 내용은 README.md 및 `documents/` 폴더 참조.

## 개발 시 주의사항

- 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`는 `.env` 파일에 설정
- 새 기능은 TDD 방식으로 개발
- 커스텀 예외는 `ErrorCode` enum에 추가 후 `CustomException` 사용
- API 생성시 OpenAPI(Swagger) Orval naming convention rule 적용하여 작성.