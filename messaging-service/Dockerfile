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

RUN mvn dependency:go-offline -pl messaging-service -am -q || true

COPY . .
RUN mvn -pl messaging-service -am clean package -DskipTests -q

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /build/messaging-service/target/messaging-service-1.0.0-SNAPSHOT.jar app.jar

ENV TZ=Asia/Ho_Chi_Minh
EXPOSE 8082
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]