# DDD 리팩토링 Task List

## 개요
Service에 있는 비즈니스 로직을 Domain으로 이동하여 DDD 원칙에 맞게 리팩토링

---

## 1순위: 핵심 도메인 (복잡한 비즈니스 로직)

### Task 1.1: ReservationService 리팩토링 ✅
- [x] `Reservation.createWithStock()` 팩토리 메서드 추가 (예매번호 생성, 총가격 계산 포함)
- [x] `ScheduleTicketStock.canReserve(quantity)`, `validateAndDecreaseStock()`, `calculateTotalPrice()` 추가
- [x] `Reservation.matchesPhone()` 연락처 검증 메서드 추가
- [x] `ReservationService`에서 Domain 메서드 호출로 변경
- [x] 빌드 및 검증 완료

### Task 1.2: SeatReservationService 리팩토링 ⏸️ (보류 - 설계 변경 예정)
- [ ] `ScheduleSeat.canReserveBy(userId, sessionId)` 검증 메서드 추가
- [ ] `Reservation.createWithSeats()` 팩토리 메서드 추가 (좌석 기반 예매)
- [ ] 연락처 정규화 로직을 도메인으로 이동
- [ ] `SeatReservationService`에서 Domain 메서드 호출로 변경
- [ ] 빌드 및 검증

### Task 1.3: ScheduleSeatService 리팩토링 ⏸️ (보류 - 설계 변경 예정)
- [ ] `ScheduleSeat.create()` 팩토리 메서드 추가
- [ ] `PerformanceSchedule.initializeSeats()` 또는 Domain Service 도입
- [ ] `ScheduleSeatService`에서 Domain 메서드 호출로 변경
- [ ] 빌드 및 검증

---

## 2순위: 중간 복잡도

### Task 2.1: AdminAuthService 리팩토링 ✅
- [x] `Admin.create()` 팩토리 메서드 추가
- [x] `Admin.validatePassword()` 검증 메서드 추가
- [x] `Admin.changePassword()`, `Admin.updateProfile()` 추가
- [x] `AdminSignupRequest.toEntity()` 제거
- [x] `AdminAuthService`에서 Domain 메서드 호출로 변경
- [x] 빌드 및 검증 완료

### Task 2.2: SeatService 리팩토링
- [ ] `Seat.create()` 팩토리 메서드 추가
- [ ] `Seat.update()` 메서드 추가 (새 엔티티 생성 패턴 제거)
- [ ] `Venue`를 Aggregate Root로 좌석 관리 검토
- [ ] `SeatService`에서 Domain 메서드 호출로 변경
- [ ] 빌드 및 검증

### Task 2.3: ScheduleTicketStockService 리팩토링
- [ ] `ScheduleTicketStock.create()` 팩토리 메서드 추가
- [ ] `ScheduleTicketStock.update()` 메서드에 검증 로직 추가
- [ ] `ScheduleTicketStockService`에서 Domain 메서드 호출로 변경
- [ ] 빌드 및 검증

---

## 3순위: 단순 개선

### Task 3.1: SeatHoldService 개선
- [ ] 최대 좌석 수 체크 로직을 도메인으로 이동
- [ ] 총 가격 계산 로직을 도메인으로 이동
- [ ] 빌드 및 검증

### Task 3.2: DTO toEntity() 제거
- [ ] `CreateVenueRequest.toEntity()` → `Venue.create()`
- [ ] `CreatePerformanceRequest.toEntity()` → `Performance.create()`
- [ ] `AdminSignupRequest.toEntity()` → `Admin.create()` (Task 2.1에서 처리)
- [ ] 빌드 및 검증

### Task 3.3: AuthService 남은 개선사항
- [ ] 이메일 중복 확인 로직 검토 (Domain Service 도입 여부)
- [ ] 빌드 및 검증

---

## 진행 상태
- 시작일: 2025-12-15
- 완료: Task 1.1 (ReservationService), Task 2.1 (AdminAuthService)
- 보류: Task 1.2, 1.3, 2.2, 2.3, 3.1 (좌석/공연장/예매 관련 - 설계 변경 예정)
- 대기: Task 3.2, 3.3 (단순 개선)
