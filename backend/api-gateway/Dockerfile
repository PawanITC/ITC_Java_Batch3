# ==================================================
# API Gateway Microservice
# ==================================================
# Author: Abbas
# Description: Central entry point for all client requests.
#              Routes requests to appropriate backend microservices,
#              handles authentication, and manages API versioning.
# ==================================================

# ---------------------------
# Stage 1: Build
# ---------------------------
FROM gradle:9.4.0-jdk17 AS builder

WORKDIR /app

# Copy Gradle wrapper and project files
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .
COPY src ./src

# Build the Spring Boot fat jar (skip tests to speed up)
RUN ./gradlew clean build -x test

# ---------------------------
# Stage 2: Runtime
# ---------------------------
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Metadata Labels
LABEL maintainer="Abbas"
LABEL service.name="api-gateway"
LABEL service.description="API Gateway - Routes requests to microservices"
LABEL service.version="1.0.0"

# Copy jar from build stage
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]