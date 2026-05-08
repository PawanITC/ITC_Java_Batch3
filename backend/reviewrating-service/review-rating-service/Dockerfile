# ==================================================
# Review Rating Service — Write side (CQRS)
# ==================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN addgroup --system spring && adduser --system spring --ingroup spring
RUN chown -R spring:spring /app
USER spring:spring

LABEL maintainer="Abbas"
LABEL service.name="review-rating-service"

COPY --chown=spring:spring reviewrating-service/review-rating-service/build/libs/review-rating-service.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
