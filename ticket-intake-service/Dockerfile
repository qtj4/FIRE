# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy parent pom.xml
COPY pom.xml .

# Copy only existing module poms that are defined in parent pom.xml
COPY ticket-intake-service/pom.xml ./ticket-intake-service/
COPY evaluation-service/pom.xml ./evaluation-service/

# Copy source code for the target module
COPY ticket-intake-service/src ./ticket-intake-service/src

# Build only the ticket-intake-service module
RUN mvn clean package -pl ticket-intake-service -am -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/ticket-intake-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
