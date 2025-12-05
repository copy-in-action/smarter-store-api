# 아키텍처 및 계층별 구조 가이드 (DDD)

이 문서는 프로젝트의 도메인 주도 설계(DDD) 기반 아키텍처와 계층별 구조에 대해 상세히 설명합니다.

## 1. 아키텍처 및 방법론

-   프로젝트는 **도메인 주도 설계(DDD)**를 따릅니다. 핵심 로직은 `domain` 패키지에, 비즈니스 로직은 `service`에, 외부와의 통신은 `controller`에 위치시켜 각 계층의 책임을 명확히 분리합니다.
-   모든 새로운 기능은 **테스트 주도 개발(TDD)** 접근 방식을 사용하여 개발되어야 합니다.

## 2. 계층별 구조 (Product 및 User/Auth 도메인 예시)

프로젝트는 다음 5가지 주요 계층으로 구성됩니다. 각 계층은 명확한 역할과 책임을 가집니다.

### 2.1. Controller 계층 (`controller` 패키지)

*   **주요 파일**: `controller/ProductController.kt`, `controller/AuthController.kt`
*   **역할**:
    -   HTTP 요청을 받고 응답하는 역할만 담당합니다.
    -   요청 데이터를 `@Valid` 어노테이션으로 검증하고, DTO를 받아 서비스 계층에 전달합니다.
    -   서비스의 응답(DTO)을 받아 `ResponseEntity`로 감싸 클라이언트에 반환합니다.
    -   보안이 필요한 엔드포인트에는 `@PreAuthorize("hasRole('ROLE_USER')")`와 같은 어노테이션으로 접근 권한을 명시합니다.
*   **책임**: 클라이언트의 요청을 받아 서비스 계층으로 전달하고, 서비스 계층의 결과를 클라이언트에게 HTTP 응답으로 변환하여 전달합니다. 직접적으로 비즈니스 로직을 처리하거나 데이터베이스에 접근하지 않습니다.

### 2.2. Service 계층 (`service` 패키지)

*   **주요 파일**: `service/ProductService.kt`, `service/AuthService.kt`, `service/CustomUserDetailsService.kt`
*   **역할**:
    -   실질적인 비즈니스 로직을 수행합니다.
    -   트랜잭션을 관리합니다. (`@Transactional`)
    -   Repository를 사용하여 데이터베이스와 상호작용합니다.
    -   요청 DTO를 영속성 엔티티로 변환하고, 엔티티를 응답 DTO로 변환합니다.
    -   예외 상황 발생 시, 미리 정의된 `CustomException`을 발생시킵니다.
*   **책임**: 도메인 엔티티를 조작하여 비즈니스 목표를 달성하고, 트랜잭션 경계를 정의합니다.

### 2.3. DTO 계층 (`dto` 패키지)

*   **주요 파일**: `dto/ProductDto.kt`, `dto/AuthDto.kt`
*   **역할**:
    -   계층 간 (특히 Controller ↔ Service) 데이터 전송을 위한 객체입니다. API의 스펙이 됩니다.
    -   요청(Request) DTO와 응답(Response) DTO를 분리하여 정의합니다.
    -   요청 DTO에는 `@NotBlank` 등과 같은 유효성 검증(Validation) 어노테이션을 포함합니다.
*   **책임**: 클라이언트와 서버 간의 데이터 형식을 정의하고 유효성을 검증하며, 내부 도메인 모델의 노출을 방지합니다.

### 2.4. Domain/Entity 계층 (`domain` 패키지)

*   **주요 파일**: `domain/Product.kt`, `domain/User.kt`, `domain/BaseEntity.kt`, `domain/Role.kt`
*   **역할**:
    -   데이터베이스 테이블과 1:1로 매핑되는 핵심 객체입니다.
    -   엔티티 자신과 관련된 비즈니스 로직(예: `update()`)을 포함할 수 있습니다.
    -   `BaseEntity`를 상속받아 `createdAt`, `updatedAt` 필드를 자동으로 관리합니다.
    -   `User` 엔티티는 사용자 인증 정보(이메일, 암호화된 비밀번호, 역할)를 저장합니다.
*   **책임**: 비즈니스 도메인의 핵심 개념과 데이터, 그리고 그 데이터가 가질 수 있는 행위(메서드)를 정의합니다.

### 2.5. Repository 계층 (`repository` 패키지)

*   **주요 파일**: `repository/ProductRepository.kt`, `repository/UserRepository.kt`
*   **역할**:
    -   `JpaRepository`를 상속받아 데이터베이스에 대한 기본적인 CRUD(Create, Read, Update, Delete) 오퍼레이션을 담당합니다.
*   **책임**: 도메인 엔티티의 영속성(Persistence)을 관리합니다. 데이터베이스 접근 로직을 추상화하여 서비스 계층이 데이터 저장 기술에 의존하지 않도록 합니다.
