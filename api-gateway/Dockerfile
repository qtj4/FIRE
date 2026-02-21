FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Копируем родительский pom
COPY pom.xml .

# Копируем модуль api-gateway
COPY api-gateway/pom.xml api-gateway/
COPY api-gateway/src api-gateway/src

# Собираем модуль api-gateway и его зависимости
RUN mvn clean package -pl api-gateway -am -DskipTests

# Финальный образ
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/api-gateway/target/*.jar app.jar

EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.jar"]
