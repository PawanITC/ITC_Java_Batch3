FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 9096
ENTRYPOINT ["java", "-jar", "app.jar"]