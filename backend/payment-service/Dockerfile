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

# Jenkins built the JAR in: backend/payment-service/build/libs/
# We copy it into the image here.
COPY build/libs/*.jar app.jar

# Standard internal port for our microservices
EXPOSE 8080

# Production-ready JVM flags for container environments
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]