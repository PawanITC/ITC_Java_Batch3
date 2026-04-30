# ==================================================
# Order Service Microservice
# ==================================================
# Author: Abbas
# Description: Handles order creation, lifecycle management,
#              and Kafka-based event publishing for Funkart.
# ==================================================

# ---------------------------
# Build Stage (Gradle)
# ---------------------------
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app

# Copy full project
COPY . .

# Make wrapper executable
RUN chmod +x gradlew

# Build JAR (skip tests for Docker build speed)
RUN ./gradlew clean build -x test

# ---------------------------
# Runtime Stage (Lean & Fast)
# ---------------------------
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Metadata Labels
LABEL maintainer="Abbas"
LABEL service.name="order-service"
LABEL service.description="Order Service - Core order processing and Kafka event publishing"

# Copy built artifact from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Service port
EXPOSE 8080

# JVM container-friendly settings
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]