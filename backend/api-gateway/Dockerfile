# ==================================================
# API Gateway Microservice
# ==================================================
# Author: Abbas
# Description: Central entry point for all client requests.
#              Routes requests to appropriate backend microservices,
#              handles authentication, and manages API versioning.
# ==================================================

# ---------------------------
# Runtime Stage (Lean & Fast)
# ---------------------------
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Metadata Labels
LABEL maintainer="Abbas"
LABEL service.name="api-gateway"
LABEL service.description="API Gateway - Entry point for Funkart"

# Copy the JAR built by Jenkins
# IMPORTANT: Jenkins must produce this exact filename
COPY build/libs/api-gateway.jar app.jar

# Gateway runs on 8080
EXPOSE 8080

# JVM container-friendly settings
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]