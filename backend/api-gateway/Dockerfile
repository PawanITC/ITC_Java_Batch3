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
# We use a wildcard to catch the versioned jar (e.g., api-gateway-0.0.1-SNAPSHOT.jar)
COPY build/libs/*.jar app.jar

# Gateway runs on 8080
EXPOSE 8080

# Best practice for JVM in containers:
# - Use MaxRAMPercentage so Java respects Docker memory limits
# - Use ContainerSupport to see CPU quotas
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]