# ==================================================
# Rating Aggregator Service — Read side (CQRS)
# ==================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN addgroup --system spring && adduser --system spring --ingroup spring
RUN chown -R spring:spring /app
USER spring:spring

LABEL maintainer="Abbas"
LABEL service.name="rating-aggregator-service"

COPY --chown=spring:spring rating-aggregator-service/build/libs/rating-aggregator-service.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
