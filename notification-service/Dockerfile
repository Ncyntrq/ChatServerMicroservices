# syntax=docker/dockerfile:1.6
# Multi-stage build cho notification-service.
# Build context = project root (vì notification-service phụ thuộc common-lib).

# ---------- Stage 1: Build ----------
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build

# COPY đầy đủ các module để không bị lỗi thiếu dependencies
COPY . .

# Copy source code 2 module cần thiết
COPY common-lib common-lib
COPY notification-service notification-service

# Build JAR, skip test (test đã chạy ở CI/local trước đó)
RUN ./mvnw -pl notification-service -am clean package -DskipTests

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Lấy JAR từ stage build
COPY --from=build /build/notification-service/target/notification-service-1.0.0-SNAPSHOT.jar app.jar

# Timezone Việt Nam
ENV TZ=Asia/Ho_Chi_Minh \
    JAVA_OPTS=""

EXPOSE 8088

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
