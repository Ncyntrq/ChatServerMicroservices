# syntax=docker/dockerfile:1.6
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build

COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && sed -i 's/\r$//' mvnw

# COPY đầy đủ các module để không bị lỗi thiếu dependencies
COPY . .

# --- SỬA TÊN MODULE TẠI ĐÂY ---
RUN ./mvnw -pl notification-service -am clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# --- SỬA TÊN MODULE TẠI ĐÂY ---
COPY --from=build /build/notification-service/target/notification-service-1.0.0-SNAPSHOT.jar app.jar

ENV TZ=Asia/Ho_Chi_Minh

# --- SỬA PORT TƯƠNG ỨNG TẠI ĐÂY ---
# Gateway: 8080 | Auth: 8081 | Messaging: 8082 | Presence: 8083 | Log: 8084 | File: 8089 | Notification: 8088
EXPOSE 8088

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]