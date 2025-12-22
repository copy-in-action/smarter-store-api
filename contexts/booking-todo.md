# 예매 시스템 구현 TODO

## 1. 도메인 레이어

- [x] **(참고) `seat` 패키지의 `ScheduleSeatStatus` 엔티티**
  - 예매 확정 시 'SOLD' 상태로 영구 저장할 대상 엔티티
- [x] **BookingStatus enum 생성**
  - PENDING, CONFIRMED, CANCELLED, EXPIRED
- [x] **Booking 엔티티 생성**
  - user, schedule, status, expiresAt, totalPrice, bookingNumber
  - create() 팩토리 메서드
  - isExpired(), getRemainingSeconds(), addSeat(), confirm(), cancel(), expire() 도메인 메서드
- [x] **BookingSeat 엔티티 생성**
  - booking, section, row, seatNumber, grade, price
- [x] **SeatLock 엔티티 생성**
  - schedule, booking, section, row_name, seatNumber, expiresAt
  - Unique 제약: (schedule_id, section, row_name, seat_number)

## 2. Repository 레이어

- [x] **BookingRepository 생성**
  - findByUserIdAndScheduleIdAndStatus()
  - findAllByStatusAndExpiresAtBefore()
- [x] **SeatLockRepository 생성**
  - deleteByBookingId()
  - deleteAllByExpiresAtBefore()

## 3. DTO

- [x] **StartBookingRequest**: `scheduleId`
- [x] **SeatRequest**: `section`, `row`, `seatNumber` (선택 및 취소 시 공통 사용)
- [x] **BookingResponse**: `bookingId`, `bookingNumber`, `expiresAt`, `remainingSeconds`, `seats`, `totalPrice`, `status`
- [x] **BookingTimeResponse**: `bookingId`, `remainingSeconds`, `expired`

## 4. Service 레이어

- [x] **BookingService 생성**
  - [x] startBooking() - 예매 시작 (5분 타이머)
  - [x] selectSeat() - 좌석 선택 (단일)
    - [x] 가격 검증 (서버 기준 데이터 확인)
    - [x] 좌석 중복 점유 체크 (SeatLock INSERT 시도)
    - [x] 인당 최대 4석 제한 검증
    - [x] 성공 시 BookingSeat 저장 및 총 금액(totalPrice) 갱신
  - [x] deselectSeat() - 좌석 선택 취소 (단일)
    - [x] SeatLock 삭제 및 BookingSeat 삭제
    - [x] 총 금액(totalPrice) 재계산
  - [x] getRemainingTime() - 남은 시간 조회
  - [x] confirmBooking() - 결제 완료 (예매 확정)
    - [x] 만료 시간 재확인
    - [x] SeatLock 삭제 및 `ScheduleSeatStatus` 'SOLD' 상태로 생성 (영구 점유)
    - [x] Booking 상태 CONFIRMED 변경
  - [x] cancelBooking() - 예매 취소 (전체)

## 5. Controller 레이어

- [x] **BookingController 생성**
  - [x] POST /api/bookings/start - 예매 시작
  - [x] POST /api/bookings/{bookingId}/seats - 좌석 선택 (개별)
  - [x] DELETE /api/bookings/{bookingId}/seats - 좌석 선택 취소 (개별)
  - [x] GET /api/bookings/{bookingId}/time - 남은 시간 조회
  - [x] POST /api/bookings/{bookingId}/confirm - 결제 완료
  - [x] DELETE /api/bookings/{bookingId} - 예매 취소 (전체)

## 6. 스케줄러

- [x] **BookingCleanupScheduler 생성**
  - [x] @Scheduled(fixedRate = 60000) - 1분마다 실행
  - [x] 만료된 PENDING 예매 → EXPIRED 상태 변경 (Bulk Update)
  - [x] 만료된 SeatLock 삭제 (Bulk Delete)

## 7. ErrorCode 추가

- [x] BOOKING_NOT_FOUND (404) - 해당 예매를 찾을 수 없습니다
- [x] BOOKING_EXPIRED (410) - 예매 시간이 만료되었습니다
- [x] BOOKING_INVALID_STATUS (400) - 유효하지 않은 예매 상태입니다
- [x] SEAT_ALREADY_OCCUPIED (409) - 이미 선택된 좌석입니다
- [x] SEAT_LIMIT_EXCEEDED (400) - 최대 선택 가능 좌석 수(4석)를 초과했습니다
- [x] PRICE_MISMATCH (400) - 요청된 가격이 서버 정보와 일치하지 않습니다

## 8. 유틸리티

- [ ] **BookingNumberGenerator 구현**
  - 날짜 + 랜덤 문자열 조합 등 고유 번호 생성 로직

## 9. 테스트

- [ ] BookingService 단위 테스트
- [ ] BookingController 통합 테스트
- [ ] 동시성 테스트 (같은 좌석 동시 선택)

---

## 향후 개선 사항

- [ ] Redis 도입 (트래픽 증가 시)
- [ ] WebSocket 실시간 좌석 상태 동기화
- [ ] 분산 락 (다중 서버 환경)
- [ ] PG사 결제 연동
