# Grafana 모니터링 매뉴얼

## 개정이력
| 버전 | 일자 | 작성자 | 내용 |
|------|------|--------|------|
| 1.0 | 2025-12-31 | Claude | 최초 작성 |

---

## 1. 접속 정보

| 항목 | 값 |
|------|-----|
| URL | http://localhost:3001 |
| 계정 | admin |
| 비밀번호 | admin |

---

## 2. 기본 용어

| 용어 | 설명 |
|------|------|
| **Dashboard** | 여러 패널을 모아놓은 모니터링 화면 |
| **Panel** | 개별 그래프/차트 단위 |
| **Data Source** | 데이터를 가져오는 소스 (Prometheus, Loki 등) |
| **PromQL** | Prometheus 쿼리 언어 |

---

## 3. 대시보드 생성하기

### 3.1 새 대시보드 생성
1. 좌측 메뉴 **+** 클릭
2. **Dashboard** 선택
3. **Add visualization** 클릭

### 3.2 패널 추가
1. **Data source**: `Prometheus` 선택
2. 하단 **Metric** 드롭다운에서 원하는 메트릭 선택
3. **Run queries** 클릭하여 데이터 확인
4. 우측 상단 **Apply** 클릭하여 저장

### 3.3 대시보드 저장
1. 우측 상단 💾 (저장) 아이콘 클릭
2. 대시보드 이름 입력
3. **Save** 클릭

---

## 4. 권장 모니터링 패널 구성

### 4.1 시스템 상태 (System Health)

#### 서비스 상태
```promql
up{job="smarter-store-api"}
```
- **Visualization**: Stat
- **용도**: 서비스 UP/DOWN 상태 (1=정상, 0=다운)

#### CPU 사용률
```promql
process_cpu_usage{job="smarter-store-api"} * 100
```
- **Visualization**: Gauge 또는 Time series
- **단위**: Percent (0-100)

#### JVM 메모리 사용량
```promql
jvm_memory_used_bytes{job="smarter-store-api", area="heap"}
```
- **Visualization**: Time series
- **단위**: bytes (IEC)

#### JVM 메모리 사용률 (%)
```promql
sum(jvm_memory_used_bytes{job="smarter-store-api", area="heap"})
/ sum(jvm_memory_max_bytes{job="smarter-store-api", area="heap"}) * 100
```
- **Visualization**: Gauge
- **단위**: Percent (0-100)
- **임계값**: 70% 주의, 85% 경고

---

### 4.2 HTTP 요청 모니터링

#### 초당 요청 수 (RPS)
```promql
rate(http_server_requests_seconds_count{job="smarter-store-api"}[5m])
```
- **Visualization**: Time series
- **용도**: 트래픽 추이 확인

#### 평균 응답 시간 (초)
```promql
rate(http_server_requests_seconds_sum{job="smarter-store-api"}[5m])
/ rate(http_server_requests_seconds_count{job="smarter-store-api"}[5m])
```
- **Visualization**: Time series
- **단위**: seconds (s)

#### HTTP 상태 코드별 요청 수
```promql
sum by(status) (rate(http_server_requests_seconds_count{job="smarter-store-api"}[5m]))
```
- **Visualization**: Time series 또는 Pie chart
- **용도**: 2xx, 4xx, 5xx 분포 확인

#### 5xx 에러 수
```promql
sum(rate(http_server_requests_seconds_count{job="smarter-store-api", status=~"5.."}[5m]))
```
- **Visualization**: Stat 또는 Time series
- **용도**: 서버 에러 추적

#### API 엔드포인트별 요청 수
```promql
topk(10, sum by(uri) (rate(http_server_requests_seconds_count{job="smarter-store-api"}[5m])))
```
- **Visualization**: Bar chart
- **용도**: 가장 많이 호출되는 API 확인

---

### 4.3 데이터베이스 연결 (HikariCP)

#### 활성 DB 연결 수
```promql
hikaricp_connections_active{job="smarter-store-api"}
```
- **Visualization**: Time series
- **용도**: 현재 사용 중인 DB 커넥션

#### 대기 중인 연결 수
```promql
hikaricp_connections_pending{job="smarter-store-api"}
```
- **Visualization**: Time series
- **용도**: 커넥션 풀 부족 징후 감지

#### 총 연결 수
```promql
hikaricp_connections{job="smarter-store-api"}
```
- **Visualization**: Gauge
- **용도**: 전체 커넥션 풀 사용량

---

### 4.4 JVM 상세

#### 활성 스레드 수
```promql
jvm_threads_live_threads{job="smarter-store-api"}
```
- **Visualization**: Time series

#### GC Pause 시간
```promql
rate(jvm_gc_pause_seconds_sum{job="smarter-store-api"}[5m])
```
- **Visualization**: Time series
- **용도**: GC로 인한 지연 모니터링

---

## 5. 패널 시각화 타입 가이드

| 타입 | 용도 |
|------|------|
| **Time series** | 시간에 따른 변화 추이 (기본) |
| **Stat** | 단일 숫자 값 표시 (UP/DOWN, 현재값) |
| **Gauge** | 백분율, 임계값이 있는 값 |
| **Bar chart** | 비교, 순위 |
| **Pie chart** | 비율 분포 |
| **Table** | 상세 데이터 목록 |

---

## 6. 알림 설정 (Alerting)

### 6.1 알림 규칙 생성
1. 패널 편집 화면에서 **Alert** 탭 클릭
2. **Create alert rule from this panel** 클릭
3. 조건 설정:
   - **When**: 값이 임계치를 넘을 때
   - **Evaluate**: 평가 주기

### 6.2 권장 알림 조건

| 항목 | 조건 | 심각도 |
|------|------|--------|
| 서비스 다운 | `up == 0` | Critical |
| CPU 사용률 | `> 80%` for 5m | Warning |
| 메모리 사용률 | `> 85%` for 5m | Warning |
| 5xx 에러 급증 | `> 10/min` | Critical |
| DB 연결 대기 | `pending > 5` for 1m | Warning |

---

## 7. 유용한 팁

### 7.1 시간 범위 조정
- 우측 상단 시간 선택기로 조회 기간 변경
- 자주 쓰는 범위: Last 15 minutes, Last 1 hour, Last 24 hours

### 7.2 자동 새로고침
- 우측 상단 🔄 아이콘 옆 드롭다운
- 권장: 10s 또는 30s

### 7.3 변수 사용
- Dashboard Settings → Variables
- 환경, 인스턴스 등을 변수로 설정하여 필터링 가능

### 7.4 대시보드 공유
1. 우측 상단 **Share** 아이콘 클릭
2. **Export** 탭에서 JSON 다운로드
3. 다른 Grafana에서 Import하여 사용

---

## 8. 트러블슈팅

| 증상 | 원인 | 해결 |
|------|------|------|
| No data | 앱이 DOWN 상태 | `docker logs smarter-store-api-local` 확인 |
| No data | Datasource 연결 실패 | Data sources → Prometheus → Test 실행 |
| 메트릭 없음 | Actuator 미노출 | `/actuator/prometheus` 엔드포인트 확인 |
| 그래프 끊김 | 앱 재시작됨 | 정상 현상, 재시작 시점에 데이터 없음 |

---

## 9. 참고 링크

- [Grafana 공식 문서](https://grafana.com/docs/grafana/latest/)
- [PromQL 기본 가이드](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Spring Boot Actuator 메트릭](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics)
