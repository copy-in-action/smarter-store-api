# 좌석 일괄 점유 및 실시간 동기화 구현 TODO

## Phase 1: SSE 인프라 구축 ✅
- [x] SSE 이벤트 DTO 정의 (`SeatEventMessage`: action, seats)
- [x] `SseService` 개발 (Emitter 관리, 45초 Heartbeat 발송, 이벤트 전송 인터페이스)
- [x] `SeatController`에 SSE 구독 엔드포인트 구현 (`/api/schedules/{id}/seats/stream`)
- [x] Security 설정 업데이트 (SSE 경로 허용 및 인증 정보 확인)

## Phase 2: BookingService 리팩토링 (일괄 점유 적용) ✅
- [x] `BookingService.startBooking` API 수정
    - 좌석 목록(`List<SeatPositionRequest>`)을 인자로 받아 일괄 점유 처리.
    - DB Unique 제약 조건을 활용한 동시성 제어 및 예외 처리 로직 추가.
    - 점유 성공 시 `SseService`를 통해 `OCCUPIED` 이벤트 발행.
- [x] 기존 개별 좌석 선택(`selectSeat`) 및 해제(`deselectSeat`) API 제거 (BookingController, BookingService).
- [x] `SeatController`의 `holdSeats`, `releaseSeats`, `reserveSeats` API 제거 (중복 기능).
- [x] `SeatService`의 불필요한 메서드 정리.

## Phase 3: 라이프사이클 관리 (확정, 취소, 만료) ✅
- [x] `confirmBooking` 시 `CONFIRMED` 이벤트 발행 로직 추가.
- [x] `cancelBooking` 및 `BookingCleanupScheduler` 만료 처리 시 좌석 해제 및 `RELEASED` 이벤트 발행.

## Phase 4: 검증 및 고도화
- [ ] **동시성 테스트**: 여러 스레드에서 동시에 `startBooking` 호출 시 1명만 성공하는지 확인.
- [ ] **SSE 연동 테스트**: 점유/해제/확정 시 브라우저에서 실시간으로 데이터가 수신되는지 확인.
- [ ] **메모리 관리**: SSE 연결 종료 시 서버 메모리(Emitter Map)가 정상적으로 정리되는지 검증.

## Phase 5: 문서 정리
- [ ] 구형 설계 문서 삭제 (`contexts/booking-sse.md`, `documents/design/SSE_실시간_통신.md`)
- [ ] 통합 설계 문서 기반으로 API 문서(Swagger) 최신화.
