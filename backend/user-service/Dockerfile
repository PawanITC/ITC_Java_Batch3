# ==================================================
# User Service - Identity & Auth
# ==================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Security: Run as non-root user
RUN addgroup --system spring && adduser --system spring --ingroup spring
RUN chown -R spring:spring /app
USER spring:spring

LABEL maintainer="Abbas"
LABEL service.name="user-service"

COPY --chown=spring:spring user-service/build/libs/*.jar app.jar

EXPOSE 8080

# Condensed to one line for readability while maintaining JVM flags
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-XX:+UseG1GC", "-jar", "app.jar"]