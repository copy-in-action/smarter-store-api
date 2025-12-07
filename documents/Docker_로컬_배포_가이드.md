# Docker를 이용한 로컬 환경 애플리케이션 배포 가이드

이 가이드는 Docker Desktop이 설치된 로컬 환경에서 Spring Boot 애플리케이션(`smarter-store-api`)을 Docker 컨테이너로 배포하는 방법을 단계별로 설명합니다.

---

### **단계별 로컬 Docker 배포 가이드**

이 가이드는 `smarter-store-api` 프로젝트를 기준으로 작성되었습니다.

#### **1단계: Spring Boot 애플리케이션 빌드**

먼저 Docker 이미지로 만들 JAR 파일을 생성해야 합니다.

*   **명령어:**
    ```bash
    ./gradlew clean build -x test
    ```
    (Windows 환경에서는 `gradlew.bat clean build -x test`를 사용하세요.)
*   **설명:**
    *   `./gradlew clean build`: 프로젝트를 클린하고 빌드하여 `build/libs` 디렉토리에 JAR 파일을 생성합니다.
    *   `-x test`: 테스트 코드를 실행하지 않고 빌드합니다. (선택 사항이며, 테스트 통과가 확실하다면 생략해도 됩니다.)
*   **결과:** `build/libs` 디렉토리에 `smarter-store-api-0.0.1-SNAPSHOT.jar` (버전은 다를 수 있음)와 같은 실행 가능한 JAR 파일이 생성됩니다.

#### **2단계: Dockerfile 생성 및 확인**

Docker 이미지를 빌드하기 위한 지시 사항을 담은 `Dockerfile`이 필요합니다. 프로젝트의 `src/main/` 디렉토리 아래에 `Dockerfile`이 없다면 다음 내용으로 생성하세요.

*   **파일:** Dockerfile
*   **내용:**
    ```dockerfile
    # OpenJDK 21 기반의 Slim 이미지를 사용합니다.
    FROM eclipse-temurin:21-jdk

    # 빌드된 JAR 파일의 경로를 인자로 받습니다.
    ARG JAR_FILE=build/libs/*.jar

    # 빌드된 JAR 파일을 컨테이너 내부의 app.jar로 복사합니다.
    COPY ${JAR_FILE} app.jar

    # Spring Boot 애플리케이션을 실행할 작업 디렉토리를 설정합니다.
    WORKDIR /app

    # 로그를 저장할 디렉토리를 볼륨으로 선언합니다.
    # 이는 'docker-compose.yml'에서 호스트 볼륨과 연결될 것입니다.
    VOLUME /app/logs

    # 애플리케이션이 사용할 포트를 외부에 노출합니다.
    EXPOSE 8080

    # 컨테이너 시작 시 애플리케이션을 실행하는 명령어를 정의합니다.
    ENTRYPOINT ["java","-jar","/app.jar"]
    ```
*   **설명:**
    *   `FROM`: 베이스 이미지를 지정합니다. Java 17을 사용하므로 `openjdk:17-jdk-slim`을 사용했습니다.
    *   `ARG JAR_FILE`: 빌드된 JAR 파일의 이름을 유연하게 처리하기 위한 변수입니다.
    *   `COPY`: 로컬의 JAR 파일을 Docker 이미지 내부로 복사합니다.
    *   `WORKDIR`: 컨테이너 내부에서 작업할 디렉토리를 `/app`으로 설정합니다. `logback-spring.xml`에서 `logs` 디렉토리를 상대 경로로 지정했기 때문에 `/app/logs`가 됩니다.
    *   `VOLUME`: `/app/logs` 디렉토리를 볼륨으로 선언하여 외부에서 마운트될 수 있음을 알립니다.
    *   `EXPOSE`: 애플리케이션이 8080 포트를 사용함을 외부에 알립니다.
    *   `ENTRYPOINT`: 컨테이너가 시작될 때 실행될 명령어를 정의합니다. 여기서는 Spring Boot JAR 파일을 실행합니다.

#### **3단계: Docker 이미지 빌드**

작성된 `Dockerfile`을 사용하여 Docker 이미지를 빌드합니다.

*   **명령어:**
    ```bash
    docker build -t smarter-store-api:latest .
    ```
*   **설명:**
    *   `docker build`: Docker 이미지를 빌드하는 명령어입니다.
    *   `-t smarter-store-api:latest`: 빌드된 이미지에 `smarter-store-api`라는 이름과 `latest` 태그를 부여합니다.
    *   `-f src/main/Dockerfile`: `Dockerfile`의 위치를 명시합니다.
    *   `.`: Docker 빌드 컨텍스트를 현재 디렉토리(프로젝트 루트)로 지정합니다.

*   **결과 확인:** 이미지가 성공적으로 빌드되었는지 확인합니다.
    ```bash
    docker images
    ```
    `smarter-store-api` 이름의 이미지가 보여야 합니다.

#### **4단계: Docker 컨테이너 실행**

빌드된 Docker 이미지를 사용하여 컨테이너를 실행합니다.

*   **환경 변수 준비 (필수):**
    `JWT_SECRET_LOCAL` 환경 변수가 필요합니다. 이 값은 이전에 로컬 환경용으로 생성한 Base64 인코딩된 Secret 키입니다.
    *   **Windows PowerShell:** `$env:JWT_SECRET_LOCAL="your_local_base64_secret"`
    *   **Linux/macOS Terminal:** `export JWT_SECRET_LOCAL="your_local_base64_secret"`
*   **로그 저장용 디렉토리 생성 (선택):**
    `docker-compose.yml`이 있는 디렉토리(프로젝트 루트)에 `logs` 디렉토리를 미리 생성해두는 것이 좋습니다.
    ```bash
    mkdir logs
    ```

*   **명령어:**
    ```powershell
    # 1. JWT_SECRET_LOCAL 환경 변수 설정 (이전에 생성한 Base64 키로 대체하세요)
    $env:JWT_SECRET_LOCAL="your_local_base64_secret_here"

    # 2. Docker 컨테이너 실행 명령어 (PowerShell용)
    # 다음 명령어를 통째로 복사해서 PowerShell에 붙여넣고 엔터키를 누르세요.
    docker run -d `
      -p 8080:8080 `
      --name smarter-store-api-local `
      -v ./logs:/app/logs `
      -e SPRING_PROFILES_ACTIVE=local `
      -e JWT_SECRET_LOCAL="$env:JWT_SECRET_LOCAL" `
      smarter-store-api:latest
    ```
*   **설명:**
    *   `$env:JWT_SECRET_LOCAL="your_local_base64_secret_here"`: 이 줄은 PowerShell 세션 내에서 `JWT_SECRET_LOCAL` 환경 변수를 설정합니다. **반드시 `"your_local_base64_secret_here"` 부분을 사용하시는 로컬 Secret 키 값으로 교체해야 합니다.**
    *   `docker run -d`: 컨테이너를 백그라운드(detached mode)에서 실행합니다.
    *   `-p 8080:8080`: 호스트 머신의 8080 포트를 컨테이너의 8080 포트에 연결합니다.
    *   `--name smarter-store-api-local`: 컨테이너에 `smarter-store-api-local`이라는 이름을 부여합니다.
    *   `-v ./logs:/app/logs`: 호스트 머신의 현재 디렉토리 아래 `logs` 폴더를 컨테이너 내부의 `/app/logs`에 마운트합니다. (로그 영속성 확보)
    *   `-e SPRING_PROFILES_ACTIVE=local`: Spring Boot 프로필을 `local`로 설정합니다.
    *   `-e JWT_SECRET_LOCAL="$env:JWT_SECRET_LOCAL"`: `JWT_SECRET_LOCAL` 환경 변수를 컨테이너 내부로 주입합니다. PowerShell의 `"$env:JWT_SECRET_LOCAL"` 문법을 사용하여 현재 세션에 설정된 값을 참조합니다.
    *   `smarter-store-api:latest`: 실행할 Docker 이미지의 이름과 태그입니다.

*   **결과 확인:** 컨테이너가 실행 중인지 확인합니다.
    ```bash
    docker ps
    ```
    `smarter-store-api-local` 컨테이너가 `Up` 상태로 표시되어야 합니다.

#### **5단계: 배포 확인**

*   **애플리케이션 접속:** 웹 브라우저나 API 클라이언트(예: Postman)를 사용하여 `http://localhost:8080`으로 접속하여 애플리케이션이 정상적으로 동작하는지 확인합니다.
*   **로그 확인:** 프로젝트 루트 디렉토리의 `logs` 폴더에 로그 파일들이 생성되고 내용이 쌓이는지 확인합니다.
    ```bash
    # 컨테이너 로그 확인 (실시간)
    docker logs -f smarter-store-api-local

    # 호스트 머신의 logs 폴더 내용 확인
    ls -l logs/
    ```

#### **6단계: (선택 사항) Docker Compose 사용**

애플리케이션과 데이터베이스 등 여러 서비스를 함께 관리해야 한다면 `docker-compose.yml`을 사용하는 것이 훨씬 편리합니다.

*   **`docker-compose.yml` 파일 생성:** 프로젝트 루트 디렉토리에 `docker-compose.yml` 파일을 다음 내용으로 생성합니다.
    ```yaml
    version: '3.8'

    services:
      smarter-store-api:
        image: smarter-store-api:latest
        container_name: smarter-store-api-local
        ports:
          - "8080:8080"
        volumes:
          - ./logs:/app/logs
        environment:
          - SPRING_PROFILES_ACTIVE=local
          - JWT_SECRET_LOCAL=${JWT_SECRET_LOCAL} # 호스트 환경 변수를 참조
        depends_on:
          - db # db 서비스가 먼저 시작되도록 의존성 설정
        restart: always

      db: # PostgreSQL 데이터베이스 예시
        image: postgres:13-alpine
        container_name: smarter-store-db-local
        environment:
          POSTGRES_DB: smarter_store_db
          POSTGRES_USER: user
          POSTGRES_PASSWORD: password
        volumes:
          - db_data:/var/lib/postgresql/data # 데이터베이스 데이터 영속성 확보
        ports:
          - "5432:5432" # 필요하다면 호스트에 노출
        restart: always

    volumes:
      db_data: # db_data 볼륨 정의
    ```
*   **환경 변수 준비:** `JWT_SECRET_LOCAL` 환경 변수가 설정되어 있어야 합니다 (위 4단계와 동일).
*   **`docker-compose` 실행:**
    ```bash
    docker-compose up -d
    ```
*   **종료:**
    ```bash
    docker-compose down
    ```
*   **컨테이너 및 볼륨 삭제:**
    ```bash
    docker-compose down --volumes
    ```

---

