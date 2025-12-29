# Grafana + Loki 모니터링 시스템 설계

## 개정이력

| 버전 | 일자 | 작성자 | 내용 |
|------|------|--------|------|
| 1.0 | 2025-12-29 | Gemini | 초기 모니터링 스택 설계 |

---

이 가이드는 Grafana 모니터링 스택(Loki, Prometheus)을 구축하여 애플리케이션 로그와 메트릭을 가시화하는 방법을 설명합니다.

## 1. 시스템 구성 (LGP 스택)

- **Grafana**: 통합 대시보드 시각화 도구.
- **Loki**: 로그 데이터 수집 및 저장소 (LogQL 지원).
- **Promtail**: 로그 파일을 읽어 Loki로 전송하는 에이전트.
- **Prometheus**: 시계열 메트릭(CPU, RAM, HTTP 요청 수 등) 수집.

## 2. 애플리케이션 설정 (Spring Boot)

### 2.1. 의존성 추가
- `spring-boot-starter-actuator`
- `micrometer-registry-prometheus`

### 2.2. 노출 설정
`/actuator/prometheus` 엔드포인트를 열어 Prometheus가 데이터를 긁어갈 수 있도록 설정합니다.

## 3. 실행 방법 (Docker Compose)

모니터링 전용 `docker-compose.yml`을 작성하여 인프라 독립적으로 실행하는 것을 권장합니다. 기본 포트는 `3000`(Grafana)을 사용합니다.