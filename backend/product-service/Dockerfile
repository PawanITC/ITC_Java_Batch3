# ==================================================
# Product Service Microservice
# ==================================================
# Author: Abbas
# Description: Manages the product catalog, categories,
#              and inventory for Funkart.
# ==================================================

# ---------------------------
# Runtime Stage (Lean & Fast)
# ---------------------------
# We use 'jre' instead of 'jdk' for the runtime to keep the image small
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Metadata Labels
LABEL maintainer="Abbas"
LABEL service.name="product-service"
LABEL service.description="Product Service - Catalog and Inventory Management"

# 1. Security: Run as a non-root user (Best Practice)
# Adding the user first so the COPY command can assign ownership
RUN useradd -ms /bin/bash springuser
RUN chown -R springuser:springuser /app

# 2. Copy the JAR built by Jenkins
# Jenkins runs './gradlew build', so we just grab the result from the workspace
COPY --chown=springuser:springuser build/libs/*.jar app.jar

# 3. Switch to the non-privileged user
USER springuser

# 4. Networking
# Product Service internal port
EXPOSE 9090

# 5. JVM container-friendly settings
# - UseContainerSupport: respects Docker memory limits
# - MaxRAMPercentage: prevents JVM from "eating" all host memory
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]