# 리팩토링 및 DDD 아키텍처 개선 과제

## 1. BookingService 리팩토링
현재 `BookingService.startBooking` 메서드가 너무 많은 책임(좌석 차이 계산, 점유 상태 관리, 예매 생성, SSE 발행)을 가지고 있어 복잡도가 높습니다. 이를 도메인 주도 설계(DDD) 원칙에 맞춰 분리해야 합니다.

- [x] **좌석 할당 로직 분리 (Priority: High)**
    - 현재 서비스 메서드 내에 있는 `keptSeats`, `releasedSeats`, `addedSeats` 계산 및 DB 처리 로직을 별도의 도메인 서비스(예: `SeatAllocationManager`) 또는 Private Method로 추출하여 가독성을 높여야 합니다.
    - **목표:** `startBooking` 메서드는 전체적인 흐름(유저 조회 -> 스케줄 조회 -> 좌석 할당 -> 예매 생성)만 표현하도록 개선.

- [x] **BookingSeat 생성 로직 캡슐화 (Priority: Medium)**
    - 반복문을 통해 `BookingSeat`를 생성하고 `addSeat`하는 로직을 `Booking` 엔티티 내부나 팩토리 메서드로 캡슐화하는 것을 고려합니다.
    - 예: `booking.addSeats(seats: List<SeatInfo>)`

- [x] **SSE 이벤트 발행 로직 추상화 (Priority: Low)**
    - SSE 발행 로직이 서비스 코드 곳곳에 산재해 있습니다. 이를 AOP나 도메인 이벤트(Domain Event) 발행 후 리스너에서 처리하는 방식으로 구조를 개선할 수 있는지 검토합니다.
    - (완료: BookingService 내에서는 Private Method로 분리하여 가독성 확보. 전역 이벤트 버스 도입은 추후 고려)

## 2. PerformanceScheduleService 개선
- [x] **TicketOption 관리 책임 위임 (Priority: Low)**
    - `updateSchedule`에서 `TicketOption`을 삭제하고 재생성하는 로직이 서비스에 노출되어 있습니다.
    - 가능하다면 `PerformanceSchedule` 애그리거트 루트를 통해 티켓 옵션을 관리하도록 개선(`schedule.replaceTicketOptions(...)`)하는 방안을 검토합니다. (단, JPA 연관관계 및 성능 이슈 고려 필요)
    - (완료: `PerformanceSchedule`에 `ticketOptions` OneToMany 추가 및 서비스 로직 위임)

## 3. 도메인 로직 강화 (Anemic Domain Model 탈피)
- [x] **검증 로직의 도메인 이동 (Priority: Medium)**
    - 서비스 계층에 있는 각종 검증 로직(예: 요청된 좌석 등급이 Venue에 존재하는지 확인하는 로직)을 도메인 엔티티(`Venue`, `Performance`) 내부로 이동하여 응집도를 높여야 합니다.
    - (완료: `SeatingChartParser.validateSeatGrades` 구현 및 적용)

---

## 리팩토링 검증 테스트 체크리스트 (2025-12-27)

### 1. 공연 회차 관리 (Admin API)
**대상 API:**
- `POST /api/admin/performances/{performanceId}/schedules` (회차 생성)
- `PUT /api/admin/performances/schedules/{scheduleId}` (회차 수정)
- `DELETE /api/admin/performances/schedules/{scheduleId}` (회차 삭제)

**체크리스트:**
- [ ] **날짜 포맷 확인:** `showDateTime`, `saleStartDateTime`이 ISO 8601 형식(`2025-12-27T14:30:00`)으로 정상 작동하는지 확인.
- [ ] **초 단위 절삭 확인:** 저장 요청 시 초 단위 이하가 `00`으로 절삭되어 저장되는지 확인.
- [ ] **중복 등록 차단:** 동일 공연, 동일 시간에 대해 `409 Conflict` (DUPLICATE_SCHEDULE) 발생 확인.
- [ ] **수정/삭제 제한:** 예매(`PENDING`/`CONFIRMED`)가 존재하는 회차 수정/삭제 시 `400 Bad Request` (SCHEDULE_ALREADY_BOOKED) 발생 확인.
- [ ] **Cascade 동작:** `TicketOption`이 정상적으로 저장/수정/삭제되는지 확인.

### 2. 좌석 상태 조회 (Public API)
**대상 API:**
- `GET /api/schedules/{scheduleId}/seat-status`

**체크리스트:**
- [ ] **응답 구조 변경:** `pending`, `reserved` 필드로 그룹화된 JSON 응답 확인.

### 3. 예매 시작 (User API)
**대상 API:**
- `POST /api/bookings/start`

**체크리스트:**
- [ ] **예매 로직 정상 작동:** 리팩토링 후에도 좌석 점유 및 예매 생성이 정상적으로 완료(`201 Created`)되는지 확인.
- [ ] **좌석 정보 저장:** `BookingSeat`의 `section`이 `"GENERAL"`로 저장되는지 확인.
- [ ] **SSE 발행:** 좌석 점유/해제 시 실시간 이벤트 수신 확인.