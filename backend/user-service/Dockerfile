# ==================================================
# User Service Microservice - Runtime Image
# ==================================================
FROM eclipse-temurin:17-jdk-jammy

# Security: Run as non-root user
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

WORKDIR /app

LABEL maintainer="Abbas"
LABEL service.name="user-service"
LABEL service.description="Core Identity Service - Auth & Profile Management"

# Jenkins builds from root, so we copy from the specific module path
# Ensure build.gradle has 'archiveFileName = "user-service.jar"'
COPY --chown=spring:spring backend/user-service/build/libs/user-service.jar app.jar

# Standard Spring Boot port
EXPOSE 8080

# JVM container-friendly settings
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-XX:+UseG1GC", \
            "-jar", "app.jar"]