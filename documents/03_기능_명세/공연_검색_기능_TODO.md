# [CCS-170] 공연 검색 기능 구현 TODO

## 1. 도메인 및 공통 (Domain & Common)
- [ ] `Region` Enum 생성 (17개 행정구역: 서울, 인천, 경기, 강원, 충북, 충남, 대전, 세종, 전북, 전남, 광주, 경북, 경남, 대구, 울산, 부산, 제주)
- [ ] 주소 문자열에서 `Region` Enum을 추출하는 매핑 유틸리티 구현 (기존 `RegionMapper` 로직 참고 및 확장)

## 2. DTO 정의 (Data Transfer Objects)
- [ ] `PerformanceAutocompleteResponse`: 자동완성 항목 (ID, 제목, 썸네일, 카테고리, 지역명)
- [ ] `PerformanceSearchRequest`: 검색 파라미터 (keyword, status, category, region, sort, page/size)
- [ ] `PerformanceSearchResponse`: 검색 결과 항목 데이터
- [ ] `PerformanceSearchListResponse`: 결과 목록(`List<PerformanceSearchResponse>`) + 필터별 상품 개수(`meta`)

## 3. 데이터 접근 계층 (Repository)
- [ ] `PerformanceRepositoryCustom` 인터페이스 및 `PerformanceRepositoryImpl` 구현체 생성
- [ ] 동적 검색 쿼리 구현 (JpaSpecificationExecutor 또는 Criteria API 사용)
    - [ ] 통합 검색: 제목, 카테고리, 공연장 주소(`OR` 조건)
    - [ ] 상태 필터: 판매 예정/중/종료 (날짜 기반 계산)
    - [ ] 장르/지역 필터 적용
    - [ ] 정렬 로직:
        - [ ] 예매 많은 순 (Booking Join 및 Count 집계)
        - [ ] 종료 임박 순 (`endDate` ASC)
        - [ ] 최근 등록 순 (`createdAt` DESC)
- [ ] 필터별 실시간 상품 개수 집계 쿼리 구현

## 4. 서비스 계층 (Service)
- [ ] `PerformanceSearchService` 신규 생성
- [ ] **검색 자동완성 로직**: 
    - [ ] 검색어 매칭 + 판매 예정/중인 공연 필터링
    - [ ] 최대 6개 결과 제한 및 경량 DTO 반환
- [ ] **검색 목록 로직**:
    - [ ] 필터 및 정렬 조건이 적용된 페이징 데이터 조회
    - [ ] 검색 결과 기반의 필터별 상품 개수(Count) 집계 데이터 결합

## 5. 컨트롤러 계층 (Controller)
- [ ] `PerformanceSearchController` 생성 및 엔드포인트 정의
    - [ ] `GET /api/performances/search/autocomplete`
    - [ ] `GET /api/performances/search`
- [ ] Swagger 어노테이션 추가 및 권한 설정 (`permitAll`)

## 6. 검증 및 테스트 (Verification)
- [ ] 검색어 및 다중 필터 조합 테스트 (단위 테스트)
- [ ] 정렬 순서 정확성 검증 (특히 예매 많은 순)
- [ ] 페이징 및 인피니티 스크롤 데이터 정합성 확인
- [ ] 17개 행정구역 주소 매핑 정확도 테스트
