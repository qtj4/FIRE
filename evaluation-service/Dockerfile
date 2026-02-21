FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Build with context = evaluation-service directory.
# Module pom has parent ../pom.xml, so we provide it from parent-pom.xml.
COPY parent-pom.xml /app/pom.xml
COPY pom.xml /app/evaluation-service/pom.xml
COPY src /app/evaluation-service/src
RUN mvn -f /app/evaluation-service/pom.xml clean package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/evaluation-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
