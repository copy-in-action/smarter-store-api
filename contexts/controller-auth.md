# 컨트롤러 권한 및 경로 규칙

## 1. API 경로 구분
- **규칙**: 관리자 권한이 필요한 모든 API는 `/api/admin/**` 경로를 사용합니다.
- **예외**: 관리자 로그인/회원가입 등 인증이 불필요한 API는 `/api/admin/auth/**`를 사용합니다.

## 2. 패키지 및 클래스 구조
- **규칙**: 관리자 전용 기능은 반드시 `admin` 패키지 하위의 별도 컨트롤러로 분리하여 구현합니다.
- **이유**: 일반 사용자용 로직과 관리자용 로직을 물리적으로 분리하여 보안 실수를 방지하고 유지보수성을 높입니다.
- **예시**:
    - 사용자용: `com.github.copyinaction.performance.controller.PerformanceController`
    - 관리자용: `com.github.copyinaction.admin.controller.AdminPerformanceController`

## 3. 보안 설정 (SecurityConfig)
- **규칙**: `SecurityConfig`에서 경로 기반으로 권한을 강제합니다.
    - `/api/admin/auth/**`: `permitAll()` (인증 불필요)
    - `/api/admin/**`: `hasRole('ADMIN')` (관리자 권한 필수)
- **이점**: 개별 컨트롤러 메서드에서 `@PreAuthorize`를 누락하더라도, 경로 수준에서 보안이 보장됩니다.
