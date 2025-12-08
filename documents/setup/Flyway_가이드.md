# Flyway 가이드: 데이터베이스 스키마 버전 관리

이 문서는 프로젝트에서 데이터베이스 스키마 변경 사항을 관리하기 위해 사용하는 **Flyway** 도구에 대한 상세한 가이드입니다.

## 1. Flyway란 무엇인가?

Flyway는 데이터베이스 마이그레이션을 위한 오픈소스 버전 관리 도구입니다. 데이터베이스 스키마의 변경 이력을 코드처럼 관리하여, 시간이 지남에 따라 데이터베이스 구조를 안정적이고 예측 가능하게 진화시킬 수 있도록 돕습니다.

## 2. Flyway를 사용하는 이유

*   **버전 관리**: 모든 스키마 변경 사항(테이블 생성, 컬럼 추가/삭제 등)을 SQL 파일 형태로 명시적으로 관리하고 버전화할 수 있습니다.
*   **재현성 및 일관성**: 개발, 테스트, 운영 등 모든 환경에서 동일한 순서와 내용으로 데이터베이스 스키마 변경을 적용할 수 있어 환경 간의 불일치를 방지합니다.
*   **협업 용이성**: 여러 개발자가 동시에 데이터베이스 스키마를 변경해야 할 때, 충돌을 최소화하고 변경 사항을 안전하게 통합할 수 있습니다.
*   **CI/CD 통합**: 지속적 통합/배포(CI/CD) 파이프라인에 쉽게 통합하여, 배포 시 자동으로 데이터베이스 스키마를 최신 상태로 유지할 수 있습니다.
*   **`ddl-auto: none` 사용**: JPA(Hibernate)의 `ddl-auto` 옵션은 개발 초기에는 편리하지만, 운영 환경에서는 예상치 못한 스키마 변경을 일으킬 수 있어 위험합니다. Flyway를 사용하면 `ddl-auto: none`으로 설정하여 Hibernate가 스키마를 건드리지 않도록 하고, Flyway가 스키마 관리를 전적으로 담당하게 함으로써 안정성을 확보할 수 있습니다.

## 3. Flyway 작동 방식

Flyway는 `src/main/resources/db/migration` 디렉터리에 있는 SQL 스크립트를 찾아 순서대로 실행하여 데이터베이스 스키마를 업데이트합니다.

### 3.1. 마이그레이션 스크립트 작성 규칙

Flyway는 마이그레이션 파일의 이름 규칙에 따라 실행 순서와 내용을 식별합니다.

*   **명명 규칙**: `V<VERSION>__<DESCRIPTION>.sql`
    *   `V`: 버전 관리 마이그레이션임을 나타내는 접두사 (대문자 V).
    *   `<VERSION>`: 고유한 버전 번호 (예: `1`, `2_1`, `202312011000`). 숫자, 밑줄(`_`)로 구분된 숫자, 날짜/시간 형식 등 자유롭게 사용할 수 있으나, **항상 오름차순으로 정렬될 수 있어야 합니다.**
    *   `__`: 버전 번호와 설명 사이의 구분자 (두 개의 밑줄).
    *   `<DESCRIPTION>`: 마이그레이션의 목적을 설명하는 텍스트 (예: `Create_product_table`). 공백 대신 밑줄(`_`)을 사용합니다.
    *   `.sql`: SQL 스크립트 파일 확장자.

*   **예시**:
    *   `V1__Create_product_table.sql`
    *   `V2__Add_email_to_user_table.sql`
    *   `V3_1__Update_product_price_column.sql`

### 3.2. `flyway_schema_history` 테이블

Flyway는 데이터베이스 내부에 `flyway_schema_history`라는 메타데이터 테이블을 자동으로 생성합니다. 이 테이블에는 어떤 마이그레이션 스크립트가 언제, 어떤 순서로 실행되었는지에 대한 이력이 기록됩니다. Flyway는 이 테이블을 참조하여 다음에 실행해야 할 스크립트가 무엇인지 판단합니다.

### 3.3. `baseline-on-migrate` 옵션

`spring.flyway.baseline-on-migrate: true` 설정은 데이터베이스에 `flyway_schema_history` 테이블이 없거나 데이터베이스가 비어있지 않을 때 유용합니다.
Flyway는 첫 번째 마이그레이션을 실행하기 전에 현재 데이터베이스 스키마를 'baseline'으로 간주하고, 이를 `flyway_schema_history` 테이블에 버전 1(또는 지정된 버전)으로 기록합니다. 이렇게 하면 Flyway가 기존 스키마를 처음부터 다시 실행하려고 시도하는 것을 방지할 수 있습니다.
**주의**: 새로 시작하는 프로젝트에서는 `false`로 두어도 무방하며, 기존 데이터베이스에 Flyway를 처음 적용할 때 주로 사용합니다.

## 4. 현재 프로젝트의 Flyway 설정

`application.yml` 파일에는 다음과 같이 Flyway 관련 설정이 포함되어 있습니다.

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: none # Hibernate의 자동 스키마 관리를 비활성화
  flyway:
    enabled: true
    baseline-on-migrate: true # 기존 DB에 첫 마이그레이션 적용 시 유용
```

이 설정으로 인해 애플리케이션 시작 시 Hibernate는 스키마를 건드리지 않으며, Flyway가 `src/main/resources/db/migration` 경로의 SQL 스크립트를 검사하여 필요한 경우 데이터베이스를 최신 버전으로 마이그레이션합니다.

## 5. 현재 적용된 마이그레이션 스크립트

현재 프로젝트에는 다음과 같은 Flyway 마이그레이션 스크립트가 존재하며, 애플리케이션 실행 시 순서대로 적용됩니다.

*   **`V1__Create_product_table.sql`**: `product` 테이블 생성
*   **`V2__Create_user_table.sql`**: `users` 테이블 생성
*   **`V3__Create_refresh_tokens_table.sql`**: `refresh_tokens` 테이블 생성
