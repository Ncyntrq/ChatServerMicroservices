# syntax=docker/dockerfile:1.6
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml ./
COPY common-lib/pom.xml common-lib/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY server-service/pom.xml server-service/pom.xml
COPY channel-service/pom.xml channel-service/pom.xml
COPY messaging-service/pom.xml messaging-service/pom.xml
COPY presence-service/pom.xml presence-service/pom.xml
COPY log-service/pom.xml log-service/pom.xml
COPY gateway-service/pom.xml gateway-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY file-service/pom.xml file-service/pom.xml
COPY user-profile-service/pom.xml user-profile-service/pom.xml
COPY role-service/pom.xml role-service/pom.xml
COPY friend-service/pom.xml friend-service/pom.xml
COPY client-app/pom.xml client-app/pom.xml

RUN mvn dependency:go-offline -pl presence-service -am -q || true

COPY . .
RUN mvn -pl presence-service -am clean package -DskipTests -q

# ======== RUNTIME STAGE (Alpine + non-root) ========
FROM bellsoft/liberica-openjre-alpine:17

# Security: chạy bằng user không phải root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app
COPY --from=build --chown=appuser:appgroup /build/presence-service/target/presence-service-1.0.0-SNAPSHOT.jar app.jar

USER appuser

ENV TZ=Asia/Ho_Chi_Minh
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

EXPOSE 8083
# exec => PID 1 nhận SIGTERM đúng (graceful shutdown)
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
