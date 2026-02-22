# Build with context = ticket-intake-service directory (like evaluation-service).
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Module pom has parent ../pom.xml, so we provide it from parent-pom.xml.
COPY parent-pom.xml /app/pom.xml
COPY pom.xml /app/ticket-intake-service/pom.xml
COPY src /app/ticket-intake-service/src

RUN mvn -f /app/ticket-intake-service/pom.xml clean package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/ticket-intake-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
