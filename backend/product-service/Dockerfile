# --- Stage 1: Build & Test (The "Kitchen") ---
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# 1. Copy Gradle wrapper and configuration files
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 2. Fix permissions and download dependencies (cached layer)
RUN chmod +x gradlew
# Pre-download dependencies to speed up subsequent builds
RUN ./gradlew dependencies --no-daemon

# 3. Copy source code
COPY src src

# 4. RUN TESTS
# If tests fail, the build stops here. No image is created.
#RUN ./gradlew test --no-daemon

# 5. Build the JAR
# We already ran tests, so we can package it now
RUN ./gradlew bootJar --no-daemon

# --- Stage 2: Runtime (The "Serving Plate") ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 6. Security: Run as a non-root user
RUN useradd -ms /bin/bash springuser
USER springuser

# 7. Copy the built JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# 8. Set the application port to 9090
ENV SERVER_PORT=9090
EXPOSE 9090

# 9. Optimized entrypoint
# The -Dserver.port=9090 ensures Spring Boot overrides any internal defaults
ENTRYPOINT ["java", "-Dserver.port=9090", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]