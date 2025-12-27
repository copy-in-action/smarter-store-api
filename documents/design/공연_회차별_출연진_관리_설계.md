# 공연 회차별 출연진(Actor) 관리 및 조회 설계

## 1. 공연별 출연진 (기본 정보)
*   **목적:** 공연 상세 페이지에서 전체 출연 배우 및 배역 정보를 보여주기 위함.
*   **구조:** 
    *   단순 문자열(`actors` 컬럼)보다는 **정규화된 관계** 권장.
    *   `Performance` (1) : (N) `PerformanceCast` (N) : (1) `Actor`
    *   `PerformanceCast` 테이블에 `role_name`(배역) 정보 저장.

## 2. 스케줄별 출연진 (회차별 캐스팅)
*   **목적:** 각 회차(Schedule)마다 실제 무대에 오르는 배우 정보를 제공.
*   **구조 제안:**
    *   기존 안: `PerformanceSchedule` 테이블에 `actors` 컬럼(JSON/String) 추가. -> **기각 (사유: 배우별 조회 성능 저하)**
    *   **개선 안:** `ScheduleCast` 테이블 별도 구성.
        *   `PerformanceSchedule` ID와 `Actor` ID (또는 `PerformanceCast` ID)를 매핑.
    *   **장점:**
        *   데이터 정합성 보장.
        *   배우 ID로 인덱스를 걸어 고성능 "배우별 스케줄 조회" 가능.

## 3. 조회 API 요구사항 및 구현 전략
*   **기능:** 배우별, 시간별, 요일별 회차 조회.
*   **구현 전략:**
    *   **배우별 조회:** `ScheduleCast` 테이블과 조인하여 `WHERE actor_id = ?` 쿼리 실행. (개선 안 채택 시 매우 간단)
    *   **시간별 조회:** `showDateTime` 컬럼 활용 (`BETWEEN` 또는 부등호 조건).
    *   **요일별 조회:** DB 함수(`DAYOFWEEK` 등) 또는 애플리케이션 레벨에서 필터링. JPQL/QueryDSL 활용 시 `FUNCTION` 호출 가능.
