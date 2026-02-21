# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy parent pom.xml
COPY pom.xml .

# Copy module poms
COPY ticket-intake-service/pom.xml ./ticket-intake-service/
COPY evaluation-service/pom.xml ./evaluation-service/

# Copy source code for evaluation-service
COPY evaluation-service/src ./evaluation-service/src

# Build only the evaluation-service module
RUN mvn clean package -pl evaluation-service -am -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/evaluation-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
