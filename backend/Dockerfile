# 빌드 컨텍스트는 저장소 루트여야 합니다.
# 예: docker build -f backend/Dockerfile -t <image-name> .

# 1. 빌드 스테이지: Gradle을 사용하여 JAR 파일 생성
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Gradle 래퍼와 멀티 모듈 설정은 저장소 루트에 있습니다.
COPY gradle /app/gradle
COPY gradlew build.gradle settings.gradle /app/
COPY backend/build.gradle /app/backend/build.gradle
COPY backend/core/core-api/build.gradle /app/backend/core/core-api/build.gradle
COPY backend/storage/db-core/build.gradle /app/backend/storage/db-core/build.gradle
COPY backend/clients/client-youtube/build.gradle /app/backend/clients/client-youtube/build.gradle
COPY backend/clients/client-mail/build.gradle /app/backend/clients/client-mail/build.gradle
COPY backend/support/monitoring/build.gradle /app/backend/support/monitoring/build.gradle

# core-enum 은 선언할 것이 없어 build.gradle 이 없습니다. 그래서 위 COPY 로는
# 디렉터리가 만들어지지 않고, gradle 이 "projectDirectory does not exist" 로 죽습니다.
RUN mkdir -p backend/core/core-enum

# 의존성 먼저 다운로드 (코드 변경 시에도 의존성 다운로드 단계는 건너뜀)
RUN chmod +x gradlew && ./gradlew :backend:core:core-api:dependencies --no-daemon

# 전체 소스 코드 복사 후 빌드
COPY backend/core/core-api/src /app/backend/core/core-api/src
COPY backend/core/core-enum/src /app/backend/core/core-enum/src
COPY backend/storage/db-core/src /app/backend/storage/db-core/src
COPY backend/clients/client-youtube/src /app/backend/clients/client-youtube/src
COPY backend/clients/client-mail/src /app/backend/clients/client-mail/src
COPY backend/support/monitoring/src /app/backend/support/monitoring/src
RUN ./gradlew :backend:core:core-api:bootJar -x test --no-daemon

# 2. 실행 스테이지: 실행에 필요한 JRE만 포함
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 파일만 가져옴
COPY --from=build /app/backend/core/core-api/build/libs/*.jar app.jar

# Spring Boot가 인식하는 프로필 환경 변수
ENV SPRING_PROFILES_ACTIVE=prod

# 포트 개방
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
