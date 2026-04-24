# ==================================================
# Payment Service Microservice
# ==================================================
# Author: Abbas
# Description: Handles all payment-related operations including payment intent
#              creation, payment confirmation, refunds, and Stripe integration.
# ==================================================

# ---------------------------
# Runtime Stage (Lean & Fast)
# ---------------------------
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Metadata Labels
LABEL maintainer="Abbas"
LABEL service.name="payment-service"
LABEL service.description="Payment Service - Handles payments via Stripe integration"

# Copy the JAR built by Jenkins
# Jenkins output: backend/payment-service/build/libs/
# Ensure Gradle bootJar produces a single predictable artifact
COPY build/libs/*.jar app.jar

# Standard internal port for microservices
EXPOSE 8080

# JVM container-friendly settings
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]