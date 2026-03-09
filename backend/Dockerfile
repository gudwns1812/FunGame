# 1. 빌드 스테이지: Gradle을 사용하여 JAR 파일 생성
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# 빌드에 필요한 파일들만 먼저 복사 (캐시 활용)
COPY gradle /app/gradle
COPY gradlew build.gradle settings.gradle /app/

# 의존성 먼저 다운로드 (코드 변경 시에도 의존성 다운로드 단계는 건너뜀)
RUN ./gradlew dependencies --no-daemon

# 전체 소스 코드 복사 후 빌드
COPY src /app/src
RUN ./gradlew clean build -x test --no-daemon

# 2. 실행 스테이지: 실행에 필요한 JRE만 포함
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 파일만 가져옴
COPY --from=build /app/build/libs/*.jar app.jar

# 환경 변수 기본값 설정 (아까 논의한 PROFILE_ACTIVE)
ENV PROFILE_ACTIVE=prod

# 포트 개방
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-Dspring.profiles.active=${PROFILE_ACTIVE}", "-jar", "app.jar"]
