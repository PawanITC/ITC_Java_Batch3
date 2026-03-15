# ==================================================
# User Service Microservice
# ==================================================
# Author: Abbas
# Description: Handles all user-related operations including registration,
#              authentication, profile management, and user data persistence.
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
LABEL service.name="user-service"
LABEL service.description="User Service - Manages user registration, authentication, and profiles"
LABEL service.version="1.0.0"

# Copy jar from build stage
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8081

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]