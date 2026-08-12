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

# RDS 서버 인증서 검증용 CA 번들 (sslMode=verify-full 에서 serverSslCert 가 가리킨다).
# ap-northeast-2 번들은 "Amazon RDS ap-northeast-2 Root CA {RSA2048,RSA4096,ECC384} G1"
# 자체 서명 루트 3장이고 JDK cacerts(amazonrootca1~4)에 없어서 반드시 따로 줘야 한다.
# 드라이버는 이 옵션에 https URL 도 받지만 그러면 물리 커넥션을 맺을 때마다 NAT 를 거쳐
# 외부로 나간다. 이미지에 구워서 DB 접속 경로에서 외부 의존을 없앴다.
# CA 교체 시 이 파일을 갱신하고 재빌드한다.
COPY certs/rds-ca-ap-northeast-2.pem /app/rds-ca.pem

# 설정 파일 디렉토리 생성
RUN mkdir -p /app/config

# 포트 노출
EXPOSE 8080

# 실행
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.location=file:/app/config/application.yml"]
