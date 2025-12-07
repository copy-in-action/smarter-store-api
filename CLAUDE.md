# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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

- DB 스키마 변경은 반드시 Flyway 마이그레이션 파일로 (`src/main/resources/db/migration/V{버전}__{설명}.sql`)
- 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`는 `.env` 파일에 설정
- 새 기능은 TDD 방식으로 개발
- 커스텀 예외는 `ErrorCode` enum에 추가 후 `CustomException` 사용
