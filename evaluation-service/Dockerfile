FROM maven:latest AS builder
WORKDIR /app

# Родительский pom и оба модуля (parent требует оба в reactor)
COPY pom.xml .
COPY ticket-intake-service/pom.xml ticket-intake-service/
COPY evaluation-service/pom.xml evaluation-service/
COPY evaluation-service/src evaluation-service/src

# Собираем только evaluation-service
RUN mvn clean package -pl evaluation-service -am -DskipTests

FROM eclipse-temurin:latest
WORKDIR /app
COPY --from=builder /app/evaluation-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]