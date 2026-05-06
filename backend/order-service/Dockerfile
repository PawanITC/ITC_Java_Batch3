# ==================================================
# Order Service - Order Lifecycle
# ==================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Security: Run as non-root user
RUN addgroup --system spring && adduser --system spring --ingroup spring
RUN chown -R spring:spring /app
USER spring:spring

LABEL maintainer="Abbas"
LABEL service.name="order-service"

COPY --chown=spring:spring order-service/build/libs/order-service.jar app.jar

EXPOSE 8080

# Condensed to one line
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]