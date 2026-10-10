# ---- Build stage ----
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
# Download dependencies first to leverage Docker layer caching
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="ATM Enterprise Team"
LABEL service="auth-service"

RUN addgroup -S atm && adduser -S atm -G atm
USER atm

WORKDIR /app
COPY --from=build /workspace/target/auth-service-1.0.0.jar /app/auth-service.jar

EXPOSE 8081

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=default

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/auth-service.jar"]
