# ==================================================
# API Gateway Microservice - Runtime Image
# ==================================================
FROM eclipse-temurin:17-jdk-jammy

# Security: Run as non-root user
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

WORKDIR /app

LABEL maintainer="Abbas"
LABEL service.name="api-gateway"
LABEL service.description="Edge Service - Routing, Security, and Rate Limiting"

# Jenkins builds from root, so we copy the specific module artifact
# Note: Ensure build.gradle has 'archiveFileName = "api-gateway.jar"'
COPY --chown=spring:spring backend/api-gateway/build/libs/api-gateway.jar app.jar

# Gateway usually maps to 8060 in your setup
EXPOSE 8060

# JVM container-friendly settings + G1GC for low-latency routing
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-XX:+UseG1GC", \
            "-jar", "app.jar"]