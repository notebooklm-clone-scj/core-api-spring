# 1. 빌드 스테이지 (소스 코드를 가져와서 Jar 파일로 굽는 과정)
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# 그래들 캐싱을 위해 설정 파일들 먼저 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 실행 권한 부여 및 의존성 다운로드 (캐시 활용)
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# 실제 소스 코드 복사 및 빌드
COPY src src
RUN ./gradlew bootJar --no-daemon

# 2. 실행 스테이지 (가벼운 JRE만 사용해서 실제 서버를 돌리는 과정)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 빌드 스테이지에서 만들어진 jar 파일만 가져오기
COPY --from=builder /app/build/libs/*.jar app.jar

# 도커 컨테이너가 열어둘 포트
EXPOSE 8080

# 스프링 부트 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]