## Stage 1: Build JAR using Gradle
#FROM eclipse-temurin:17-jdk-jammy AS build
#
#WORKDIR /app
#COPY . .
#
#RUN chmod +x gradlew
#RUN ./gradlew build -x test
#
## Stage 2: Run app
#FROM eclipse-temurin:17-jdk-jammy
#
#WORKDIR /app
#
#COPY --from=build /app/build/libs/*.jar app.jar
#
#ENTRYPOINT ["java", "-jar", "app.jar"]
#
#
#
## Build stage
#FROM maven:3.8.1-openjdk-17 AS builder
#
#WORKDIR /app
#
#COPY pom.xml .
#RUN mvn dependency:go-offline
#
#COPY src ./src
#RUN mvn clean package -DskipTests
#
## Runtime stage
#FROM openjdk:17-slim
#
#WORKDIR /app
#
#COPY --from=builder /app/target/order-service*.jar app.jar
#
#EXPOSE 8081
#
#HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
#    CMD curl -f http://localhost:8081/health || exit 1
#
#ENTRYPOINT ["java", "-jar", "app.jar"]


# Build stage
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app

# Copy gradle files first (better layer caching)
COPY build.gradle .
COPY settings.gradle .
COPY gradlew .
COPY gradle ./gradle

# Download dependencies first (cached separately)
RUN gradle dependencies --no-daemon || true

# Copy source and build
COPY src1 ./src
RUN gradle clean bootJar --no-daemon -x test

# Run stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
