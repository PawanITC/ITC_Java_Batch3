# ==================================================
# Payment Service Microservice
# ==================================================
# Author: Abbas
# Description: Handles all payment-related operations including payment intent
#              creation, payment confirmation, refunds, and Stripe integration.
# ==================================================

# ---------------------------
# Stage 1: Build
# ---------------------------
FROM gradle:9.4.0-jdk17 AS builder

WORKDIR /app

# Copy project files
COPY build.gradle .
COPY settings.gradle .
COPY src ./src

# Build the Spring Boot fat jar (skip tests to speed up)
RUN gradle clean build -x test

# ---------------------------
# Stage 2: Runtime
# ---------------------------
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Metadata Labels
LABEL maintainer="Abbas"
LABEL service.name="payment-service"
LABEL service.description="Payment Service - Handles payments via Stripe integration"
LABEL service.version="1.0.0"

# Copy jar from build stage
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8080

# Run the jar
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]