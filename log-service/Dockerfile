# syntax=docker/dockerfile:1.6
# Multi-stage build cho log-service.
# Build context = project root (vì log-service phụ thuộc common-lib).

# ---------- Stage 1: Build ----------
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build

# Copy Maven Wrapper + parent POM trước → tận dụng Docker cache layer
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Copy source code 2 module cần thiết
COPY common-lib common-lib
COPY log-service log-service

# Build JAR, skip test (test đã chạy ở CI/local trước đó)
RUN ./mvnw -pl log-service -am clean package -DskipTests

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Lấy JAR từ stage build
COPY --from=build /build/log-service/target/log-service-1.0.0-SNAPSHOT.jar app.jar

# Timezone Việt Nam
ENV TZ=Asia/Ho_Chi_Minh \
    JAVA_OPTS=""

EXPOSE 8084

# Log ghi vào /app/logs/chat_log.txt — mount volume khi chạy docker-compose
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
