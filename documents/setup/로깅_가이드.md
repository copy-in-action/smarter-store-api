# 로깅 가이드 (Logging Guide)

이 문서에서는 Smarter Store API 프로젝트의 로깅 정책, 설정 요약, 로거 사용 예시 및 외부 로그 경로 설정에 대해 설명합니다.

## 6.1. 주요 정책
- **파일 출력**: 로그는 콘솔과 파일 모두에 출력됩니다.
- **날짜 기반 롤링**: 매일 새로운 로그 파일이 생성됩니다. (`...-yyyy-MM-dd` 형식)
- **용량 기반 롤링**: 하루 동안 로그 파일이 `10MB`를 초과하면, 순번이 붙은 새 파일(`...-yyyy-MM-dd.0.log`, `...-yyyy-MM-dd.1.log`)이 생성됩니다.
- **보관 기간**: 로그 파일은 최대 `30일`까지 보관됩니다.
- **저장 경로**: 로그 파일은 프로젝트 루트의 `logs` 디렉터리에 저장됩니다.

## 6.2. 설정 요약 (`logback-spring.xml`)
- **`SizeAndTimeBasedRollingPolicy`**: 날짜와 용량 기반 롤링을 동시에 처리합니다.
- **`<fileNamePattern>`**: 로그 파일의 이름 규칙을 정의합니다.
- **`<maxHistory>`**: 보관할 로그 파일의 최대 일수를 `30`으로 설정합니다.
- **`<maxFileSize>`**: 한 파일의 최대 크기를 `10MB`로 설정합니다.
- **로그 레벨**: 전체 `root` 레벨은 `INFO`이지만, 프로젝트 패키지(`com.github.copyinaction`)는 `DEBUG` 레벨로 설정되어 더 상세한 로그를 확인할 수 있습니다.

## 6.3. 로거 사용 예시

로거를 사용하는 코드는 이전과 동일합니다. `LoggerFactory`를 통해 로거 인스턴스를 가져와 사용하면, `logback-spring.xml`에 정의된 정책에 따라 로그가 기록됩니다.

```kotlin
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MyService {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun doSomething() {
        logger.debug("상세 디버그 정보...")
        logger.info("작업을 시작합니다...")
        logger.warn("주의가 필요한 상황입니다!")
        logger.error("오류가 발생했습니다!", RuntimeException("샘플 오류"))
    }
}
```

## 6.4. 외부 로그 경로 설정

애플리케이션 로그는 프로젝트 배포 디렉토리와 독립적으로 관리하기 위해 외부 경로에 저장되도록 설정되었으며, Spring Profiles를 통해 환경별로 관리됩니다.

*   **`application-prod.yml` 설정 (운영 환경):**
    `src/main/resources/application-prod.yml` 파일에 다음 설정이 추가되었습니다.
    ```yaml
    logging:
      file:
        path: /home/cic/logs/smarter-store/
    ```
    운영 환경의 로그는 지정된 절대 경로에 기록됩니다.

*   **`application-local.yml` 설정 (로컬 개발 환경):**
    `src/main/resources/application-local.yml` 파일에 다음 설정이 추가되었습니다.
    ```yaml
    logging:
      file:
        path: ./logs/smarter-store-local/
    ```
    로컬 개발 환경의 로그는 프로젝트 루트 아래의 상대 경로에 기록됩니다.

*   **서버 설정:**
    로그를 저장할 디렉토리(예: `/home/cic/logs/smarter-store/`)는 애플리케이션을 실행하는 사용자(`cic`)가 쓰기 권한을 가져야 합니다. 원격 서버에서 다음 명령어를 실행하여 디렉토리를 생성하고 권한을 설정해야 합니다.
    ```bash
    mkdir -p /home/cic/logs/smarter-store/
    chown cic:cic /home/cic/logs/smarter-store/
    ```
