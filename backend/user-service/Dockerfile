# ==================================================
# User Service Microservice
# ==================================================
# Author: Abbas
# Description: Handles authentication, user profiles,
#              and user-related operations for Funkart.
# ==================================================

# ---------------------------
# Runtime Stage (Lean & Fast)
# ---------------------------
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Metadata Labels
LABEL maintainer="Abbas"
LABEL service.name="user-service"
LABEL service.description="User Service - Core identity and profile management"

# Copy the JAR built by Jenkins
# IMPORTANT: Ensure bootJar is configured to produce consistent name if you switch away from wildcard
COPY build/libs/*.jar app.jar

# Service runs on 8080 internally (mapped externally to 8081 in docker-compose)
EXPOSE 8080

# JVM container-friendly settings
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]