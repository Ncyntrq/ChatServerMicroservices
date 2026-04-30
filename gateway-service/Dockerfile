# syntax=docker/dockerfile:1.6

# Build stage
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build

COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && sed -i 's/\r$//' mvnw

# Copy TẤT CẢ các module được khai báo trong pom.xml gốc
COPY common-lib common-lib
COPY gateway-service gateway-service
COPY log-service log-service
COPY presence-service presence-service

# Build — Maven giờ đã thấy đủ "anh em" nên sẽ không báo lỗi nữa
RUN ./mvnw -pl gateway-service -am clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /build/gateway-service/target/gateway-service-1.0.0-SNAPSHOT.jar app.jar

ENV TZ=Asia/Ho_Chi_Minh
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]