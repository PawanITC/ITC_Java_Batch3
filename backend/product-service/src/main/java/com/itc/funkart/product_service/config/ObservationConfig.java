package com.itc.funkart.product_service.config;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservationConfig {
    @Bean
    public OtlpGrpcSpanExporter otlpHttpSpanExporter() {
        return OtlpGrpcSpanExporter.builder()
                .setEndpoint("http://localhost:4317")
                .build();
    }
}
