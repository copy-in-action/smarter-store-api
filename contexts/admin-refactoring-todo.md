# 관리자 API 리팩토링 및 보안 강화 Task List

## 보안 설정 강화
- [x] **SecurityConfig 경로별 권한 설정**
    - [x] `/api/admin/auth/**`: `permitAll()` (인증 없이 접근 가능) - 이미 적용됨
    - [x] `/api/admin/**`: `hasRole('ADMIN')` (관리자 권한 필수) 규칙 추가
    - [ ] 설정 변경 후 `AdminAuthController` 접근 테스트 (로그인/회원가입 정상 작동 여부)

## 컨트롤러 분리 작업

### PerformanceController에서 ADMIN 기능 분리
- [x] `AdminPerformanceController` 생성 (`admin.controller` 패키지)
- [x] 공연 생성/수정/삭제 API를 `/api/admin/performances`로 이동
- [x] 기존 `PerformanceController`에는 조회 API만 유지

### VenueController에서 ADMIN 기능 분리
- [x] `AdminVenueController` 생성 (`admin.controller` 패키지)
- [x] 공연장 생성/수정/삭제/좌석배치도 수정 API를 `/api/admin/venues`로 이동
- [x] 기존 `VenueController`에는 조회 API만 유지

### 기존 Admin 컨트롤러 패키지 이동
- [x] `PerformanceScheduleController` → `AdminPerformanceScheduleController`로 이름 변경 후 `admin.controller` 패키지로 이동
- [x] `CompanyController` → `AdminCompanyController`로 이름 변경 후 `admin.controller` 패키지로 이동

## API 문서화
- [x] **Swagger/OpenAPI 설정 확인**
    - [x] 관리자 API가 Swagger UI에서 명확히 구분되도록 `@Tag` 설정 완료
        - `admin-performance`: 관리자용 공연 API
        - `admin-venue`: 관리자용 공연장 API
        - `admin-performance-schedule`: 관리자용 공연 회차 API
        - `admin-company`: 관리자용 판매자 API
