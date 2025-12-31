# Smarter Store API

Kotlin + Spring Boot로 구축된 백엔드 REST API 서버입니다.
DDD(도메인 주도 설계)와 TDD(테스트 주도 개발) 원칙을 따릅니다.

## 기술 스택

- **언어**: Kotlin
- **프레임워크**: Spring Boot 3.3.0 (Spring Security, Spring Data JPA)
- **빌드 도구**: Gradle (Kotlin DSL)
- **데이터베이스**: PostgreSQL
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

---

## 📂 프로젝트 포트폴리오 개요: 고성능 공연 예매 플랫폼

### 1. 프로젝트 목적 (Why)
*   **핵심 목표:** 대규모 트래픽이 몰리는 공연 예매 환경에서 **데이터 무결성(동시성 제어)**을 보장하고, 사용자에게 **실시간 예매 현황**을 안정적으로 제공하기 위함.
*   **개발 배경:** 단순 CRUD를 넘어, 복잡한 비즈니스 로직(좌석 선점, 결제 타임아웃, 취소 등)을 객체 지향적으로 해결하고 유지보수성을 높이는 **DDD(Domain-Driven Design) 아키텍처**를 실무 수준으로 구현해보고자 함.

### 2. 시스템 구조 (Architecture & Stack)
*   **Layered Architecture (DDD 지향):** `Presentation` -> `Service` -> `Domain` -> `Infrastructure`로 의존성을 관리. 비즈니스 로직을 `Domain` 계층(Entity)에 응집시켜 서비스 계층이 비대해지는 것(Transaction Script 패턴)을 방지.
*   **Event-Driven (SSE):** 클라이언트 폴링(Polling)의 부하를 줄이기 위해 **Server-Sent Events(SSE)**를 도입하여 대기열 및 예약 상태 변경을 실시간 스트리밍.
*   **AOP 기반 Audit:** 핵심 비즈니스 로직과 로깅/감사 관심사를 분리하기 위해 Spring AOP를 사용하여 `Auditable` 어노테이션 기반 로깅 시스템 구축.

### 3. 주요 기술적 의사결정 및 문제 해결 (My Judgments)

#### ① 도메인 주도 설계(DDD)와 Kotlin의 활용
*   **판단:** Java 대신 **Kotlin**을 선택하고 엔티티에 비즈니스 로직을 위임.
*   **이유:** `data class`, `nullable type` 등 Kotlin의 문법적 설탕을 이용해 NPE를 원천 차단하고 불변성을 확보하기 위함. 또한, 단순 Setter 사용을 지양하고 `venue.addSeat()`와 같은 **행위 중심의 메서드**를 엔티티에 작성하여 도메인의 의도를 명확히 함.

#### ② UX와 리소스 효율을 위한 SSE 도입 및 예외 처리 고도화
*   **상황:** 예약 진행 상황을 사용자에게 알리기 위해 지속적인 확인이 필요.
*   **판단:** 클라이언트의 반복 요청(Polling)은 서버 리소스를 낭비하므로, **SSE(Server-Sent Events)**를 도입.
*   **트러블슈팅 (Deep Dive):**
    *   *문제:* SSE 연결(`text/event-stream`) 중 예외 발생 시, Spring이 `ErrorResponse`(JSON)를 반환하려다 `HttpMessageNotWritableException` 발생.
    *   *해결:* `AsyncRequestTimeoutException`을 별도로 잡아 `204 No Content`로 처리하여 불필요한 로그 제거. 동시에 `GlobalExceptionHandler`의 모든 에러 응답에 명시적으로 `.contentType(MediaType.APPLICATION_JSON)`을 설정하여, 스트림 연결 중에도 클라이언트가 명확한 JSON 에러 메시지를 받을 수 있도록 개선.

#### ③ 확장성을 고려한 표준화된 예외 처리 전략
*   **판단:** `GlobalExceptionHandler`와 커스텀 `ErrorCode` Enum을 통한 중앙 집중식 에러 관리.
*   **이유:** 프론트엔드와의 협업 효율성을 위해 에러 응답 포맷을 통일. 단순 500 에러가 아닌, 비즈니스상 어떤 위반이 발생했는지(예: `SEAT_ALREADY_BOOKED`) 명확한 코드를 전달하여 디버깅 및 사용자 경험 개선.

#### ④ AOP를 활용한 선언적 감사(Audit) 로그
*   **판단:** 관리자 기능의 보안을 위해 모든 변경 사항 추적 필요. 비즈니스 로직마다 로깅 코드를 넣는 중복 제거 필요.
*   **해결:** 커스텀 어노테이션 `@Auditable`과 AOP를 활용하여, 메서드 실행 전후에 자동으로 변경 주체와 내용을 기록하는 시스템 구현. **횡단 관심사(Cross-cutting Concern)**를 깔끔하게 분리.

### 4. 성과 (Results)
*   복잡한 예매 프로세스에서도 데이터 정합성을 유지하는 견고한 백엔드 구축.
*   SSE 도입을 통해 불필요한 트래픽을 감소시키고 실시간성을 확보.
*   단위 테스트(JUnit5, MockK)와 통합 테스트를 통해 핵심 로직의 안정성 검증.