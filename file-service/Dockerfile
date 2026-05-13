# syntax=docker/dockerfile:1.6
# Multi-stage build cho file-service.
# Build context = project root (vì file-service phụ thuộc common-lib).

# ---------- Stage 1: Build ----------
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build

# COPY đầy đủ các module để không bị lỗi thiếu dependencies
COPY . .

# Copy source code 2 module cần thiết
COPY common-lib common-lib
COPY file-service file-service

# Build JAR, skip test (test đã chạy ở CI/local trước đó)
RUN ./mvnw -pl file-service -am clean package -DskipTests

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Lấy JAR từ stage build
COPY --from=build /build/file-service/target/file-service-1.0.0-SNAPSHOT.jar app.jar

# Timezone Việt Nam
ENV TZ=Asia/Ho_Chi_Minh \
    JAVA_OPTS=""

EXPOSE 8089

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
