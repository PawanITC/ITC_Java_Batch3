package com.itc.funkart.product_service.config;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <h2>ObservationConfig</h2>
 * <p>
 * This configuration initializes the OpenTelemetry (OTEL) infrastructure for distributed tracing.
 * It enables the Product Service to participate in a "global trace," allowing developers to
 * track a single request as it travels through the Gateway, to the Product Service,
 * and across the Kafka bus.
 * </p>
 * * <p>
 * <b>Protocol:</b> OTLP over gRPC (Industry Standard) <br>
 * <b>Default Port:</b> 4317
 * </p>
 */
@Configuration
public class ObservationConfig {

    /**
     * Configures the Span Exporter to push trace data to a centralized collector.
     * <p>
     * <b>Note:</b> The endpoint is currently set to localhost for development. In a
     * containerized environment (Docker/K8s), this should be replaced with the
     * internal service name (e.g., http://otel-collector:4317).
     * </p>
     *
     * @return An {@link OtlpGrpcSpanExporter} configured for the local OTEL collector.
     */
    @Bean
    public OtlpGrpcSpanExporter otlpHttpSpanExporter() {
        return OtlpGrpcSpanExporter.builder()
                .setEndpoint("http://localhost:4317")
                // Added a common timeout to prevent the service from hanging
                // if the collector is temporarily unavailable.
                .setTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }
}