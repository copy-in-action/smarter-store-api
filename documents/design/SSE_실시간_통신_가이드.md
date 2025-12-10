# SSE (Server-Sent Events) 실시간 통신 가이드

## 1. SSE란?

### 1.1 개념
**SSE (Server-Sent Events)**는 서버에서 클라이언트로 실시간 데이터를 **단방향**으로 전송하는 웹 기술입니다.

```
┌─────────┐                          ┌─────────┐
│ 클라이언트 │  ──── HTTP 요청 ────▶  │   서버   │
│ (브라우저) │  ◀──── 이벤트 스트림 ──  │         │
│         │  ◀──── 이벤트 스트림 ──  │         │
│         │  ◀──── 이벤트 스트림 ──  │         │
└─────────┘       (지속적 전송)       └─────────┘
```

### 1.2 다른 실시간 기술과 비교

| 기술 | 방향 | 연결 | 프로토콜 | 사용 사례 |
|------|------|------|---------|----------|
| **SSE** | 단방향 (서버→클라이언트) | 지속 연결 | HTTP | 알림, 피드, 좌석 상태 |
| **WebSocket** | 양방향 | 지속 연결 | WS/WSS | 채팅, 게임, 협업 도구 |
| **Polling** | 단방향 | 매번 새 연결 | HTTP | 간단한 업데이트 |
| **Long Polling** | 단방향 | 요청당 대기 | HTTP | 호환성 필요 시 |

### 1.3 SSE를 선택한 이유

좌석 예매 시스템에서 SSE를 선택한 이유:

1. **단방향 통신으로 충분** - 서버가 좌석 상태 변경을 클라이언트에게 알려주기만 하면 됨
2. **HTTP 기반** - 별도 프로토콜 불필요, 기존 인프라 활용
3. **자동 재연결** - 브라우저가 연결 끊김 시 자동으로 재연결 시도
4. **간단한 구현** - WebSocket보다 구현이 단순함
5. **방화벽 친화적** - 일반 HTTP 트래픽으로 인식

---

## 2. SSE 동작 원리

### 2.1 연결 흐름

```
[클라이언트]                              [서버]
     │                                      │
     │  1. GET /api/.../events              │
     │  Accept: text/event-stream           │
     │─────────────────────────────────────▶│
     │                                      │
     │  2. HTTP 200 OK                      │
     │  Content-Type: text/event-stream     │
     │◀─────────────────────────────────────│
     │                                      │
     │  3. 연결 유지 (Keep-Alive)            │
     │  ◀ ─ ─ ─ ─ 연결 열림 ─ ─ ─ ─ ─ ─ ─ ▶ │
     │                                      │
     │  4. data: {"seatId":1,"status":"HELD"}│
     │◀─────────────────────────────────────│
     │                                      │
     │  5. data: {"seatId":2,"status":"HELD"}│
     │◀─────────────────────────────────────│
     │                                      │
     │  ... (이벤트 계속 수신)               │
     │                                      │
```

### 2.2 이벤트 형식

SSE 이벤트는 텍스트 기반 형식을 사용합니다:

```
event: seatStatusChange
data: {"scheduleSeatId":1,"status":"HELD","timestamp":"2025-01-15T14:30:00"}

event: seatStatusChange
data: {"scheduleSeatId":2,"status":"RESERVED","timestamp":"2025-01-15T14:30:05"}

: heartbeat (주석 - 연결 유지용)

data: {"scheduleSeatId":-1,"status":null,"isHeartbeat":true}
```

- `event:` - 이벤트 타입 (선택)
- `data:` - 실제 데이터 (JSON)
- `id:` - 이벤트 ID (재연결 시 사용)
- `:` - 주석 (무시됨, 연결 유지용)

---

## 3. 프로젝트 내 SSE 구현

### 3.1 아키텍처

```
┌───────────────────────────────────────────────────────────────────┐
│                         Spring Boot Server                        │
│  ┌─────────────────┐    ┌──────────────────┐    ┌──────────────┐ │
│  │ SeatHoldService │───▶│ SeatEventService │───▶│   Reactor    │ │
│  │                 │    │                  │    │    Sinks     │ │
│  │ - holdSeats()   │    │ - publishEvent() │    │              │ │
│  │ - releaseSeats()│    │                  │    │  [Schedule1] │ │
│  └─────────────────┘    └──────────────────┘    │  [Schedule2] │ │
│                                                  │  [Schedule3] │ │
│  ┌─────────────────────┐                        └──────┬───────┘ │
│  │ SeatEventController │◀───────────────────────────────┘        │
│  │                     │         Flux<SeatStatusEvent>           │
│  │ GET /seats/events   │                                         │
│  └──────────┬──────────┘                                         │
└─────────────┼─────────────────────────────────────────────────────┘
              │ text/event-stream
              ▼
┌─────────────────────────┐
│   클라이언트 (브라우저)    │
│                         │
│   EventSource API       │
│   - onmessage           │
│   - onerror             │
└─────────────────────────┘
```

### 3.2 핵심 컴포넌트

#### SeatEventService (이벤트 발행)

```kotlin
@Service
class SeatEventService {

    // 회차별로 독립적인 이벤트 채널 관리
    private val scheduleSinks = ConcurrentHashMap<Long, Sinks.Many<SeatStatusEvent>>()

    // 클라이언트가 구독할 이벤트 스트림 반환
    fun getSeatEventStream(scheduleId: Long): Flux<SeatStatusEvent> {
        val sink = scheduleSinks.computeIfAbsent(scheduleId) {
            // multicast: 여러 구독자에게 동시에 전송
            // onBackpressureBuffer: 버퍼링으로 데이터 유실 방지
            Sinks.many().multicast().onBackpressureBuffer()
        }

        return sink.asFlux()
            // 30초마다 하트비트 전송 (연결 유지)
            .mergeWith(
                Flux.interval(Duration.ofSeconds(30))
                    .map { SeatStatusEvent(isHeartbeat = true, ...) }
            )
    }

    // 좌석 상태 변경 시 이벤트 발행
    fun publishSeatStatusChange(scheduleId: Long, seatId: Long, status: SeatStatus) {
        scheduleSinks[scheduleId]?.tryEmitNext(
            SeatStatusEvent(scheduleSeatId = seatId, status = status, ...)
        )
    }
}
```

#### SeatEventController (SSE 엔드포인트)

```kotlin
@RestController
@RequestMapping("/api/schedules/{scheduleId}/seats")
class SeatEventController(
    private val seatEventService: SeatEventService
) {

    // produces = TEXT_EVENT_STREAM_VALUE가 핵심!
    @GetMapping("/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamSeatEvents(@PathVariable scheduleId: Long): Flux<SeatStatusEvent> {
        return seatEventService.getSeatEventStream(scheduleId)
    }
}
```

### 3.3 데이터 흐름

```
사용자 A가 좌석 점유
        │
        ▼
┌───────────────────┐
│  SeatHoldService  │
│  holdSeats()      │
└─────────┬─────────┘
          │ 1. DB 업데이트 (좌석 상태 → HELD)
          │
          │ 2. 이벤트 발행
          ▼
┌───────────────────┐
│ SeatEventService  │
│ publishEvent()    │
└─────────┬─────────┘
          │ 3. Sink에 이벤트 전송
          ▼
┌───────────────────┐
│   Reactor Sink    │
│  (회차별 채널)     │
└─────────┬─────────┘
          │ 4. 구독자들에게 브로드캐스트
          ├──────────────────┬──────────────────┐
          ▼                  ▼                  ▼
     [사용자 B]          [사용자 C]         [사용자 D]
     (좌석 선택 화면)     (좌석 선택 화면)    (좌석 선택 화면)

     "좌석 1번이         "좌석 1번이         "좌석 1번이
      회색으로 변경"      회색으로 변경"       회색으로 변경"
```

---

## 4. 클라이언트 구현 (JavaScript)

### 4.1 기본 사용법

```javascript
// EventSource 객체 생성 - 연결 시작
const eventSource = new EventSource('/api/schedules/1/seats/events');

// 메시지 수신 핸들러
eventSource.onmessage = (event) => {
    const data = JSON.parse(event.data);

    // 하트비트는 무시
    if (data.isHeartbeat) {
        console.log('Heartbeat received');
        return;
    }

    // 좌석 상태 업데이트
    updateSeatUI(data.scheduleSeatId, data.status);
};

// 연결 성공
eventSource.onopen = () => {
    console.log('SSE connection established');
};

// 에러 처리
eventSource.onerror = (error) => {
    console.error('SSE error:', error);

    // 브라우저가 자동으로 재연결 시도함
    // 필요시 수동으로 연결 종료
    if (eventSource.readyState === EventSource.CLOSED) {
        console.log('Connection closed');
    }
};

// 페이지 이탈 시 연결 종료
window.addEventListener('beforeunload', () => {
    eventSource.close();
});
```

### 4.2 React 예시

```jsx
import { useEffect, useState } from 'react';

function SeatMap({ scheduleId }) {
    const [seats, setSeats] = useState([]);

    useEffect(() => {
        // 초기 좌석 데이터 로드
        fetch(`/api/schedules/${scheduleId}/seats`)
            .then(res => res.json())
            .then(data => setSeats(data));

        // SSE 연결
        const eventSource = new EventSource(
            `/api/schedules/${scheduleId}/seats/events`
        );

        eventSource.onmessage = (event) => {
            const data = JSON.parse(event.data);
            if (data.isHeartbeat) return;

            // 해당 좌석 상태만 업데이트 (불변성 유지)
            setSeats(prevSeats =>
                prevSeats.map(seat =>
                    seat.id === data.scheduleSeatId
                        ? { ...seat, status: data.status }
                        : seat
                )
            );
        };

        // 클린업
        return () => eventSource.close();
    }, [scheduleId]);

    return (
        <div className="seat-map">
            {seats.map(seat => (
                <Seat
                    key={seat.id}
                    seat={seat}
                    disabled={seat.status !== 'AVAILABLE'}
                />
            ))}
        </div>
    );
}
```

### 4.3 좌석 상태별 UI 처리

```javascript
function updateSeatUI(seatId, status) {
    const seatElement = document.querySelector(`[data-seat-id="${seatId}"]`);
    if (!seatElement) return;

    // 기존 상태 클래스 제거
    seatElement.classList.remove('available', 'held', 'reserved');

    switch (status) {
        case 'AVAILABLE':
            seatElement.classList.add('available');
            seatElement.disabled = false;
            break;
        case 'HELD':
            seatElement.classList.add('held');
            seatElement.disabled = true;  // 다른 사람이 선택 중
            break;
        case 'RESERVED':
            seatElement.classList.add('reserved');
            seatElement.disabled = true;  // 예매 완료
            break;
    }
}
```

```css
/* 좌석 상태별 스타일 */
.seat.available {
    background-color: #4CAF50;  /* 녹색 - 선택 가능 */
    cursor: pointer;
}

.seat.held {
    background-color: #FFC107;  /* 노란색 - 선택 중 */
    cursor: not-allowed;
}

.seat.reserved {
    background-color: #9E9E9E;  /* 회색 - 예매 완료 */
    cursor: not-allowed;
}
```

---

## 5. Reactor 핵심 개념

### 5.1 Flux와 Mono

Spring WebFlux에서 사용하는 리액티브 타입:

```
┌──────────────────────────────────────────────────────────────┐
│                          Flux<T>                             │
│                                                              │
│  0개 ~ N개의 요소를 비동기적으로 발행하는 스트림                  │
│                                                              │
│  ──○──○──○──○──○──○──│                                      │
│    1   2   3   4   5   6  완료                               │
│                                                              │
│  예: Flux.just(1, 2, 3)                                      │
│      Flux.interval(Duration.ofSeconds(1))                    │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                          Mono<T>                             │
│                                                              │
│  0개 또는 1개의 요소를 비동기적으로 발행                         │
│                                                              │
│  ──○──│                                                      │
│    1  완료                                                    │
│                                                              │
│  예: Mono.just("Hello")                                      │
│      Mono.empty()                                            │
└──────────────────────────────────────────────────────────────┘
```

### 5.2 Sinks (이벤트 발행자)

Sinks는 프로그래밍 방식으로 Flux에 데이터를 넣을 수 있게 해줍니다:

```kotlin
// Sink 생성
val sink: Sinks.Many<String> = Sinks.many()
    .multicast()           // 여러 구독자에게 전송
    .onBackpressureBuffer() // 버퍼링

// 데이터 발행 (여러 번 호출 가능)
sink.tryEmitNext("Event 1")
sink.tryEmitNext("Event 2")
sink.tryEmitNext("Event 3")

// 구독자들은 Flux로 받음
val flux: Flux<String> = sink.asFlux()

// 구독자 1
flux.subscribe { println("구독자1: $it") }

// 구독자 2
flux.subscribe { println("구독자2: $it") }
```

### 5.3 Sink 종류

```kotlin
// 1. Unicast - 단일 구독자만 허용
Sinks.many().unicast().onBackpressureBuffer()

// 2. Multicast - 여러 구독자 허용 (우리가 사용)
Sinks.many().multicast().onBackpressureBuffer()

// 3. Replay - 새 구독자에게 이전 이벤트도 전송
Sinks.many().replay().limit(10)  // 최근 10개 이벤트 보관
```

---

## 6. 하트비트 (Heartbeat)

### 6.1 하트비트가 필요한 이유

```
문제 상황:
┌─────────┐                    ┌─────────┐
│ 클라이언트 │ ◀── 연결 ────────  │   서버   │
└─────────┘                    └─────────┘
     │                              │
     │  (이벤트 없이 시간 경과...)      │
     │                              │
     │  ← 프록시/방화벽이 유휴 연결 끊음 │
     ✕ 연결 끊김 (감지 못함)           │
```

### 6.2 하트비트로 해결

```kotlin
// 30초마다 하트비트 전송
.mergeWith(
    Flux.interval(Duration.ofSeconds(30))
        .map {
            SeatStatusEvent(
                scheduleSeatId = -1,    // 특수 ID
                status = null,
                isHeartbeat = true      // 하트비트 표시
            )
        }
)
```

```
해결:
┌─────────┐                         ┌─────────┐
│ 클라이언트 │ ◀── 연결 ─────────────  │   서버   │
└─────────┘                         └─────────┘
     │                                   │
     │  ◀─ 하트비트 (30초)                │
     │  ◀─ 하트비트 (60초)                │
     │  ◀─ 하트비트 (90초)                │
     │                                   │
     │  프록시가 활성 연결로 인식           │
     ✓ 연결 유지                          │
```

---

## 7. 에러 처리 및 재연결

### 7.1 브라우저 자동 재연결

EventSource는 연결 끊김 시 자동으로 재연결을 시도합니다:

```javascript
eventSource.onerror = (error) => {
    switch (eventSource.readyState) {
        case EventSource.CONNECTING: // 0
            console.log('재연결 시도 중...');
            break;
        case EventSource.OPEN:       // 1
            console.log('연결 활성');
            break;
        case EventSource.CLOSED:     // 2
            console.log('연결 종료됨 - 수동 재연결 필요');
            // 필요시 새 EventSource 생성
            break;
    }
};
```

### 7.2 수동 재연결 구현

```javascript
class SeatEventManager {
    constructor(scheduleId) {
        this.scheduleId = scheduleId;
        this.eventSource = null;
        this.retryCount = 0;
        this.maxRetries = 5;
    }

    connect() {
        this.eventSource = new EventSource(
            `/api/schedules/${this.scheduleId}/seats/events`
        );

        this.eventSource.onopen = () => {
            this.retryCount = 0;  // 성공시 카운터 리셋
        };

        this.eventSource.onerror = () => {
            if (this.eventSource.readyState === EventSource.CLOSED) {
                this.reconnect();
            }
        };

        this.eventSource.onmessage = (event) => {
            // ... 이벤트 처리
        };
    }

    reconnect() {
        if (this.retryCount >= this.maxRetries) {
            console.error('최대 재연결 시도 횟수 초과');
            return;
        }

        this.retryCount++;
        const delay = Math.min(1000 * Math.pow(2, this.retryCount), 30000);

        console.log(`${delay}ms 후 재연결 시도 (${this.retryCount}/${this.maxRetries})`);

        setTimeout(() => this.connect(), delay);
    }

    disconnect() {
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
        }
    }
}
```

---

## 8. 주의사항 및 Best Practices

### 8.1 서버 측

```kotlin
// 1. 메모리 관리 - 더 이상 필요 없는 스트림 정리
fun cleanupScheduleStream(scheduleId: Long) {
    scheduleSinks.remove(scheduleId)?.tryEmitComplete()
}

// 2. 백프레셔 처리 - 버퍼 오버플로우 방지
Sinks.many().multicast().onBackpressureBuffer(1000)  // 최대 1000개 버퍼

// 3. 타임아웃 설정
.timeout(Duration.ofMinutes(30))  // 30분 후 연결 종료

// 4. 에러 복구
.onErrorResume { e ->
    logger.error("SSE error", e)
    Flux.empty()  // 에러 시 빈 스트림 반환
}
```

### 8.2 클라이언트 측

```javascript
// 1. 페이지 이탈 시 연결 종료 필수
window.addEventListener('beforeunload', () => {
    eventSource.close();
});

// 2. SPA에서 라우트 변경 시 정리
useEffect(() => {
    const es = new EventSource(...);
    return () => es.close();  // 클린업
}, []);

// 3. 불필요한 재렌더링 방지
// 이벤트마다 전체 상태 교체 대신 개별 좌석만 업데이트

// 4. 연결 상태 표시
if (eventSource.readyState === EventSource.CONNECTING) {
    showLoadingIndicator();
}
```

### 8.3 인프라 설정

```nginx
# Nginx SSE 설정
location /api/schedules/*/seats/events {
    proxy_pass http://backend;
    proxy_http_version 1.1;
    proxy_set_header Connection '';
    proxy_buffering off;           # 버퍼링 비활성화 (중요!)
    proxy_cache off;
    proxy_read_timeout 3600s;      # 긴 타임아웃
    chunked_transfer_encoding off;
}
```

---

## 9. SSE vs WebSocket 선택 기준

| 상황 | 추천 | 이유 |
|------|------|------|
| 좌석 상태 알림 | **SSE** | 서버→클라이언트 단방향 |
| 실시간 채팅 | WebSocket | 양방향 통신 필요 |
| 주식 시세 | **SSE** | 서버가 데이터 푸시 |
| 협업 문서 편집 | WebSocket | 양방향 + 복잡한 동기화 |
| 알림 시스템 | **SSE** | 단순 알림 푸시 |
| 멀티플레이어 게임 | WebSocket | 저지연 양방향 통신 |

---

## 10. 테스트 방법

### 10.1 curl로 SSE 테스트

```bash
# SSE 스트림 구독
curl -N -H "Accept: text/event-stream" \
     http://localhost:8080/api/schedules/1/seats/events

# 출력 예시:
# data:{"scheduleSeatId":-1,"status":null,"timestamp":"...","isHeartbeat":true}
# data:{"scheduleSeatId":5,"status":"HELD","timestamp":"...","isHeartbeat":false}
```

### 10.2 브라우저 개발자 도구

1. Network 탭 열기
2. `/seats/events` 요청 찾기
3. EventStream 탭에서 이벤트 확인

### 10.3 Postman

- 새 요청 생성
- GET 선택, URL 입력
- Send 버튼 옆 드롭다운에서 "Send and Download" 선택
- 실시간으로 이벤트 수신 확인

---

## 11. 요약

```
┌─────────────────────────────────────────────────────────────┐
│                    SSE 핵심 요약                             │
├─────────────────────────────────────────────────────────────┤
│  1. SSE = 서버 → 클라이언트 단방향 실시간 통신                 │
│  2. HTTP 기반, 자동 재연결, 방화벽 친화적                     │
│  3. Spring WebFlux의 Flux + Sinks로 구현                    │
│  4. 하트비트로 연결 유지 (30초)                               │
│  5. 클라이언트는 EventSource API 사용                        │
│  6. 좌석 예매처럼 "서버가 푸시"하는 상황에 적합                 │
└─────────────────────────────────────────────────────────────┘
```
