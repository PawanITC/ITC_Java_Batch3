# Stage 1: Build JAR using Gradle
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app
COPY . .

RUN chmod +x gradlew
RUN ./gradlew build -x test

# Stage 2: Run app
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
