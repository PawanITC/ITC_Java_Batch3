# ==================================================
# API Gateway Microservice - Runtime Image
# ==================================================
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

LABEL maintainer="Abbas"
LABEL service.name="api-gateway"

# IMPORTANT: Path updated to reflect the root build context
# Jenkins should run: ./gradlew :api-gateway:bootJar 
# which places the jar in backend/api-gateway/build/libs/
COPY backend/api-gateway/build/libs/api-gateway.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]