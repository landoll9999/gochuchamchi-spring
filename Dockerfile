# ===== 빌드 스테이지 =====
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Maven Wrapper 또는 pom.xml 복사
COPY pom.xml .
COPY src ./src

# Maven 설치 및 빌드
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

# ===== 실행 스테이지 =====
FROM eclipse-temurin:17-jre-alpine
RUN apk --no-cache upgrade
WORKDIR /app

# 빌드된 jar 복사
COPY --from=builder /app/target/gochuchamchi-0.0.1-SNAPSHOT.jar app.jar

# 설정 파일 디렉토리 생성
RUN mkdir -p /app/config

# 포트 노출
EXPOSE 8080

# 실행
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.location=file:/app/config/application.yml"]
