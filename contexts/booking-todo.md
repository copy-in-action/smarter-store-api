# 예매 시스템 구현 TODO

## 1. 도메인 레이어

- [ ] **BookingStatus enum 생성**
  - PENDING, CONFIRMED, CANCELLED, EXPIRED

- [ ] **Booking 엔티티 생성**
  - user, schedule, status, expiresAt, totalPrice, bookingNumber
  - create() 팩토리 메서드
  - isExpired(), getRemainingSeconds(), addSeat(), confirm(), cancel(), expire() 도메인 메서드

- [ ] **BookingSeat 엔티티 생성**
  - booking, section, row, seatNumber, grade, price

- [ ] **SeatLock 엔티티 생성**
  - schedule, booking, section, row_name, seatNumber, expiresAt
  - Unique 제약: (schedule_id, section, row_name, seat_number)

## 2. Repository 레이어

- [ ] **BookingRepository 생성**
  - findByUserIdAndScheduleIdAndStatus()
  - findAllByStatusAndExpiresAtBefore()

- [ ] **SeatLockRepository 생성**
  - deleteByBookingId()
  - deleteAllByExpiresAtBefore()

## 3. DTO

- [ ] **StartBookingRequest** - scheduleId
- [ ] **SeatSelectionRequest** - seats (section, row, seatNumber, grade, price)
- [ ] **BookingResponse** - bookingId, bookingNumber, expiresAt, remainingSeconds, seats, totalPrice, status
- [ ] **BookingTimeResponse** - bookingId, remainingSeconds, expired

## 4. Service 레이어

- [ ] **BookingService 생성**
  - startBooking() - 예매 시작 (5분 타이머)
  - selectSeats() - 좌석 선택 (SeatLock 생성)
  - getRemainingTime() - 남은 시간 조회
  - confirmBooking() - 결제 완료 (예매 확정)
  - cancelBooking() - 예매 취소

## 5. Controller 레이어

- [ ] **BookingController 생성**
  - POST /api/bookings/start - 예매 시작
  - POST /api/bookings/{bookingId}/seats - 좌석 선택
  - GET /api/bookings/{bookingId}/time - 남은 시간 조회
  - POST /api/bookings/{bookingId}/confirm - 결제 완료
  - DELETE /api/bookings/{bookingId} - 예매 취소

## 6. 스케줄러

- [ ] **BookingCleanupScheduler 생성**
  - @Scheduled(fixedRate = 60000) - 1분마다 실행
  - 만료된 PENDING 예매 → EXPIRED 상태 변경
  - 만료된 SeatLock 삭제

## 7. ErrorCode 추가

- [ ] BOOKING_NOT_FOUND (404) - 해당 예매를 찾을 수 없습니다
- [ ] BOOKING_EXPIRED (410) - 예매 시간이 만료되었습니다
- [ ] BOOKING_INVALID_STATUS (400) - 유효하지 않은 예매 상태입니다

## 8. 테스트

- [ ] BookingService 단위 테스트
- [ ] BookingController 통합 테스트
- [ ] 동시성 테스트 (같은 좌석 동시 선택)

---

## 향후 개선 사항

- [ ] Redis 도입 (트래픽 증가 시)
- [ ] WebSocket 실시간 좌석 상태 동기화
- [ ] 분산 락 (다중 서버 환경)
- [ ] PG사 결제 연동
