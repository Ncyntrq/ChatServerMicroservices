# syntax=docker/dockerfile:1.6
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build

COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && sed -i 's/\r$//' mvnw

# COPY các module để Maven không báo lỗi "thiếu thành viên"
COPY common-lib common-lib
COPY gateway-service gateway-service 
COPY log-service log-service
COPY presence-service presence-service

# Build module Presence
RUN ./mvnw -pl presence-service -am clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /build/presence-service/target/presence-service-1.0.0-SNAPSHOT.jar app.jar

ENV TZ=Asia/Ho_Chi_Minh
EXPOSE 8083
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]