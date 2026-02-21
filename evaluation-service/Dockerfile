FROM maven:latest AS builder
WORKDIR /app

COPY pom.xml .
COPY evaluation-service/pom.xml evaluation-service/
COPY evaluation-service/src evaluation-service/src
RUN mvn clean package -DskipTests

# Собираем конкретный модуль
FROM eclipse-temurin:latest
WORKDIR /app
COPY --from=builder /app/evaluation-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]