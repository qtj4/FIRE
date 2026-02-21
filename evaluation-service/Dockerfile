FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
COPY evaluation-service/pom.xml evaluation-service/
COPY evaluation-service/src evaluation-service/src
COPY ticket-intake-service/pom.xml ticket-intake-service/
COPY api-gateway/pom.xml api-gateway/
RUN mvn clean package -pl evaluation-service -am -DskipTests

# Собираем конкретный модуль
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/evaluation-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]