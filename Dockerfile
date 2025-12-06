# OpenJDK 21 기반의 Slim 이미지를 사용합니다.
# 📌 ARM64 환경을 명시적으로 지정하여, Orange Pi 5에서 'exec format error'를 방지합니다.
FROM --platform=linux/arm64 eclipse-temurin:21-jdk

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