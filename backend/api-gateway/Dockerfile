# ==================================================
# API Gateway - Edge Service
# ==================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Security: Run as non-root user
RUN addgroup --system spring && adduser --system spring --ingroup spring
RUN chown -R spring:spring /app
USER spring:spring

LABEL maintainer="Abbas"
LABEL service.name="api-gateway"

# Path relative to 'backend' root
COPY --chown=spring:spring api-gateway/build/libs/api-gateway.jar app.jar

EXPOSE 8060

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-XX:+UseG1GC", "-Djava.net.preferIPv4Stack=true", "-jar", "app.jar"]