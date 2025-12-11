# Grafana + Loki 모니터링 스택 구축 가이드

이 가이드는 Orange Pi (또는 유사한 ARM64 서버)에 Grafana 모니터링 스택을 구축하는 방법을 설명합니다.

---

## 1. 개요

### 구성 요소

| 도구 | 용도 | 포트 | RAM |
|------|------|------|-----|
| **Grafana** | 대시보드 시각화 | 3000 | ~256MB |
| **Loki** | 로그 수집/저장/검색 | 3100 | ~512MB |
| **Promtail** | 로그 → Loki 전송 | - | ~50MB |
| **Prometheus** | 메트릭 수집 | 9090 | ~300MB |
| **Node Exporter** | 서버 시스템 메트릭 | 9100 | ~20MB |

**총 예상 RAM 사용량: ~1.2GB**

### 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                        Orange Pi Server                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐       │
│  │ Spring Boot │────▶│  Promtail   │────▶│    Loki     │       │
│  │   (logs)    │     │ (log agent) │     │ (log store) │       │
│  └─────────────┘     └─────────────┘     └──────┬──────┘       │
│         │                                        │              │
│         │ /actuator/prometheus                   │              │
│         ▼                                        ▼              │
│  ┌─────────────┐                         ┌─────────────┐       │
│  │ Prometheus  │◀────────────────────────│   Grafana   │       │
│  │  (metrics)  │                         │ (dashboard) │       │
│  └──────┬──────┘                         └─────────────┘       │
│         │                                       ▲              │
│         │                                       │              │
│  ┌──────▼──────┐                               │              │
│  │Node Exporter│───────────────────────────────┘              │
│  │(system info)│                                               │
│  └─────────────┘                                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 사전 준비

### 2.1. 디렉토리 구조 생성

```bash
# 모니터링 스택 디렉토리 생성
mkdir -p ~/monitoring/{grafana,loki,promtail,prometheus}
cd ~/monitoring
```

### 2.2. Spring Boot Actuator 설정

`build.gradle.kts`에 의존성 추가:

```kotlin
dependencies {
   implementation("org.springframework.boot:spring-boot-starter-actuator")
   runtimeOnly("io.micrometer:micrometer-registry-prometheus")
}
```

`application.yml` 또는 `application-prod.yml`에 추가:

```yaml
management:
   endpoints:
      web:
         exposure:
            include: health, info, prometheus, metrics
   endpoint:
      health:
         show-details: always
   prometheus:
      metrics:
         export:
            enabled: true
```

---

## 3. 설정 파일 작성

### 3.1. Loki 설정

`~/monitoring/loki/loki-config.yml`:

```yaml
auth_enabled: false

server:
   http_listen_port: 3100

ingester:
   lifecycler:
      ring:
         kvstore:
            store: inmemory
         replication_factor: 1
   chunk_idle_period: 5m
   chunk_retain_period: 30s

schema_config:
   configs:
      - from: 2020-10-24
        store: boltdb-shipper
        object_store: filesystem
        schema: v11
        index:
           prefix: index_
           period: 24h

storage_config:
   boltdb_shipper:
      active_index_directory: /loki/index
      cache_location: /loki/cache
      shared_store: filesystem
   filesystem:
      directory: /loki/chunks

limits_config:
   enforce_metric_name: false
   reject_old_samples: true
   reject_old_samples_max_age: 168h

compactor:
   working_directory: /loki/compactor
   shared_store: filesystem
```

### 3.2. Promtail 설정

`~/monitoring/promtail/promtail-config.yml`:

```yaml
server:
   http_listen_port: 9080
   grpc_listen_port: 0

positions:
   filename: /tmp/positions.yaml

clients:
   - url: http://loki:3100/loki/api/v1/push

scrape_configs:
   # Docker 컨테이너 로그 수집
   - job_name: docker
     static_configs:
        - targets:
             - localhost
          labels:
             job: docker
             __path__: /var/lib/docker/containers/*/*-json.log
     pipeline_stages:
        - json:
             expressions:
                log: log
                stream: stream
                time: time
        - labels:
             stream:
        - timestamp:
             source: time
             format: RFC3339Nano
        - output:
             source: log
```

### 3.3. Prometheus 설정

`~/monitoring/prometheus/prometheus.yml`:

```yaml
global:
   scrape_interval: 15s
   evaluation_interval: 15s

scrape_configs:
   # Prometheus 자체 메트릭
   - job_name: 'prometheus'
     static_configs:
        - targets: ['localhost:9090']

   # Node Exporter (서버 시스템 메트릭)
   - job_name: 'node-exporter'
     static_configs:
        - targets: ['node-exporter:9100']

   # Spring Boot Actuator (애플리케이션 메트릭)
   - job_name: 'spring-boot'
     metrics_path: '/actuator/prometheus'
     static_configs:
        - targets: ['smarter-store-api:8080']  # Spring Boot 컨테이너명:포트
```

---

## 4. Docker Compose 설정

### 4.1. 모니터링 전용 docker-compose.yml

`~/monitoring/docker-compose.yml`:

```yaml
version: '3.8'

services:
   # Grafana - 대시보드
   grafana:
      image: grafana/grafana:latest
      container_name: grafana
      ports:
         - "3000:3000"
      volumes:
         - grafana-data:/var/lib/grafana
      environment:
         - GF_SECURITY_ADMIN_USER=admin
         - GF_SECURITY_ADMIN_PASSWORD=admin123  # 변경 권장
         - GF_USERS_ALLOW_SIGN_UP=false
      restart: unless-stopped
      networks:
         - monitoring

   # Loki - 로그 저장소
   loki:
      image: grafana/loki:latest
      container_name: loki
      ports:
         - "3100:3100"
      volumes:
         - ./loki/loki-config.yml:/etc/loki/local-config.yaml
         - loki-data:/loki
      command: -config.file=/etc/loki/local-config.yaml
      restart: unless-stopped
      networks:
         - monitoring

   # Promtail - 로그 수집 에이전트
   promtail:
      image: grafana/promtail:latest
      container_name: promtail
      volumes:
         - ./promtail/promtail-config.yml:/etc/promtail/config.yml
         - /var/lib/docker/containers:/var/lib/docker/containers:ro
         - /var/run/docker.sock:/var/run/docker.sock
      command: -config.file=/etc/promtail/config.yml
      restart: unless-stopped
      networks:
         - monitoring

   # Prometheus - 메트릭 수집
   prometheus:
      image: prom/prometheus:latest
      container_name: prometheus
      ports:
         - "9090:9090"
      volumes:
         - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
         - prometheus-data:/prometheus
      command:
         - '--config.file=/etc/prometheus/prometheus.yml'
         - '--storage.tsdb.path=/prometheus'
         - '--storage.tsdb.retention.time=15d'
      restart: unless-stopped
      networks:
         - monitoring

   # Node Exporter - 서버 시스템 메트릭
   node-exporter:
      image: prom/node-exporter:latest
      container_name: node-exporter
      ports:
         - "9100:9100"
      volumes:
         - /proc:/host/proc:ro
         - /sys:/host/sys:ro
         - /:/rootfs:ro
      command:
         - '--path.procfs=/host/proc'
         - '--path.sysfs=/host/sys'
         - '--path.rootfs=/rootfs'
         - '--collector.filesystem.mount-points-exclude=^/(sys|proc|dev|host|etc)($$|/)'
      restart: unless-stopped
      networks:
         - monitoring

volumes:
   grafana-data:
   loki-data:
   prometheus-data:

networks:
   monitoring:
      driver: bridge
```

### 4.2. 기존 앱과 네트워크 연결

Spring Boot 앱이 별도 docker-compose로 실행 중이라면, 같은 네트워크에 연결해야 합니다.

**옵션 A: 외부 네트워크 사용**

모니터링 docker-compose.yml 하단에 추가:

```yaml
networks:
   monitoring:
      external: true
      name: smarter-store-network  # 기존 앱 네트워크명
```

**옵션 B: Prometheus에서 호스트 IP 사용**

`prometheus.yml`에서:

```yaml
- job_name: 'spring-boot'
  metrics_path: '/actuator/prometheus'
  static_configs:
     - targets: ['host.docker.internal:8080']  # 또는 실제 IP
```

---

## 5. 실행

### 5.1. 모니터링 스택 시작

```bash
cd ~/monitoring
docker-compose up -d
```

### 5.2. 상태 확인

```bash
# 컨테이너 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f grafana
docker-compose logs -f loki
docker-compose logs -f prometheus
```

### 5.3. 접속 확인

| 서비스 | URL | 기본 계정 |
|--------|-----|-----------|
| Grafana | http://{서버IP}:3000 | admin / admin123 |
| Prometheus | http://{서버IP}:9090 | - |
| Loki | http://{서버IP}:3100/ready | - |

---

## 6. Grafana 설정

### 6.1. 데이터 소스 추가

1. Grafana 접속 (http://{서버IP}:3000)
2. 좌측 메뉴 → **Connections** → **Data sources** → **Add data source**

**Prometheus 추가:**
- Type: `Prometheus`
- URL: `http://prometheus:9090`
- **Save & Test** 클릭

**Loki 추가:**
- Type: `Loki`
- URL: `http://loki:3100`
- **Save & Test** 클릭

### 6.2. 대시보드 Import

Grafana에서 제공하는 커뮤니티 대시보드를 가져올 수 있습니다.

1. 좌측 메뉴 → **Dashboards** → **Import**
2. Dashboard ID 입력 후 **Load**

**추천 대시보드 ID:**

| 대시보드 | ID | 용도 |
|----------|-----|------|
| Node Exporter Full | `1860` | 서버 시스템 모니터링 |
| Spring Boot Statistics | `12900` | Spring Boot 앱 메트릭 |
| JVM (Micrometer) | `4701` | JVM 메트릭 |
| Loki Dashboard | `13639` | 로그 검색/분석 |

### 6.3. 로그 탐색

1. 좌측 메뉴 → **Explore**
2. 상단에서 데이터 소스를 **Loki** 선택
3. Label filters에서 컨테이너 선택
4. **Run query** 클릭

**예시 쿼리:**
```
{container="smarter-store-api"} |= "ERROR"
```

---

## 7. 유용한 명령어

### 로그 확인

```bash
# 전체 로그
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f grafana
docker-compose logs -f loki
```

### 재시작

```bash
# 전체 재시작
docker-compose restart

# 특정 서비스만 재시작
docker-compose restart prometheus
```

### 중지 및 삭제

```bash
# 중지
docker-compose down

# 볼륨 포함 삭제 (데이터 삭제됨)
docker-compose down -v
```

### 리소스 사용량 확인

```bash
docker stats
```

---

## 8. 트러블슈팅

### Prometheus가 Spring Boot 메트릭을 수집하지 못할 때

1. Spring Boot 앱에서 `/actuator/prometheus` 엔드포인트 접근 확인:
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

2. 네트워크 연결 확인:
   ```bash
   docker exec prometheus ping smarter-store-api
   ```

3. `prometheus.yml`의 targets 주소 확인

### Loki에 로그가 안 들어올 때

1. Promtail 로그 확인:
   ```bash
   docker-compose logs promtail
   ```

2. Docker 로그 경로 권한 확인:
   ```bash
   ls -la /var/lib/docker/containers/
   ```

### Grafana 접속이 안 될 때

1. 포트 확인:
   ```bash
   docker-compose ps
   netstat -tlnp | grep 3000
   ```

2. 방화벽 확인:
   ```bash
   sudo ufw status
   sudo ufw allow 3000
   ```

---

## 9. 보안 권장사항

### 9.1. Grafana 비밀번호 변경

최초 로그인 후 반드시 비밀번호를 변경하세요.

### 9.2. 외부 접근 제한

프로덕션 환경에서는 Prometheus, Loki 포트를 외부에 노출하지 마세요:

```yaml
# docker-compose.yml에서 포트 매핑 제거 또는 localhost만
prometheus:
   ports:
      - "127.0.0.1:9090:9090"  # localhost에서만 접근
```

### 9.3. 리버스 프록시 사용

Nginx 등을 통해 HTTPS 및 인증을 추가하는 것을 권장합니다.

---

## 10. 참고 링크

- [Grafana 공식 문서](https://grafana.com/docs/grafana/latest/)
- [Loki 공식 문서](https://grafana.com/docs/loki/latest/)
- [Prometheus 공식 문서](https://prometheus.io/docs/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
