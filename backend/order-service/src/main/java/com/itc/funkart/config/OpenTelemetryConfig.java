////package com.itc.funkart.config;
////
////import io.opentelemetry.api.trace.Tracer;
////import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
////import io.opentelemetry.sdk.OpenTelemetrySdk;
////import io.opentelemetry.sdk.resources.Resource;
////import io.opentelemetry.sdk.trace.SdkTracerProvider;
////import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
////import io.opentelemetry.sdk.resources.Resource;
////import io.opentelemetry.api.common.Attributes;
////import io.opentelemetry.semconv.ResourceAttributes;
////import org.springframework.context.annotation.Bean;
////import org.springframework.context.annotation.Configuration;
////import jakarta.annotation.PreDestroy;
////
////@Configuration
////public class OpenTelemetryConfig {
////
////    private SdkTracerProvider tracerProvider;
////
////    @Bean
////    public Tracer tracer() {
////        // Use OTLP gRPC exporter instead of deprecated JaegerGrpcSpanExporter
////        OtlpGrpcSpanExporter otlpExporter = OtlpGrpcSpanExporter.builder()
////                .setEndpoint("http://18.134.196.21:4317")  // ✅ correct
////                .build();
////
////        // Add resource attributes to identify the service
////        Resource resource = Resource.getDefault()
////                .merge(Resource.create(Attributes.of(
////                        ResourceAttributes.SERVICE_NAME, "funkart-order-service"
////                )));
////
////        tracerProvider = SdkTracerProvider.builder()
////                .addSpanProcessor(BatchSpanProcessor.builder(otlpExporter).build()) // BatchSpanProcessor is preferred over SimpleSpanProcessor
////                .setResource(resource)
////                .build();
////
////        // Avoid buildAndRegisterGlobal() if called multiple times (e.g., during tests or hot reload)
////        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
////                .setTracerProvider(tracerProvider)
////                .build();
////
////        return openTelemetry.getTracer("com.itc.funkart.OrderService");
////    }
////
////    @PreDestroy
////    public void shutdown() {
////        if (tracerProvider != null) {
////            tracerProvider.close();
////        }
////    }
////}
////
////
////
//
////package com.itc.funkart.config;
////import io.opentelemetry.api.trace.Tracer;
////import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
////import io.opentelemetry.sdk.OpenTelemetrySdk;
////import io.opentelemetry.sdk.resources.Resource;
////import io.opentelemetry.sdk.trace.SdkTracerProvider;
////import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
////import io.opentelemetry.api.common.Attributes;
////import io.opentelemetry.semconv.ResourceAttributes;
////import org.springframework.context.annotation.Bean;
////import org.springframework.context.annotation.Configuration;
////import jakarta.annotation.PreDestroy;
////
////@Configuration
////public class OpenTelemetryConfig {
////
////    private SdkTracerProvider tracerProvider;
////
////    @Bean
////    public Tracer tracer() {
////        // ✅ Send traces to local Collector
////        OtlpGrpcSpanExporter otlpExporter = OtlpGrpcSpanExporter.builder()
////                .setEndpoint("http://localhost:4317")  // Collector endpoint
////                .build();
////
////        // Add resource attributes to identify the service
////        Resource resource = Resource.getDefault()
////                .merge(Resource.create(Attributes.of(
////                        ResourceAttributes.SERVICE_NAME, "funkart-order-service"
////                )));
////
////        tracerProvider = SdkTracerProvider.builder()
////                .addSpanProcessor(BatchSpanProcessor.builder(otlpExporter).build()) // Batch processor
////                .setResource(resource)
////                .build();
////
////        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
////                .setTracerProvider(tracerProvider)
////                .build();
////
////        return openTelemetry.getTracer("com.itc.funkart.OrderService");
////    }
////
////    @PreDestroy
////    public void shutdown() {
////        if (tracerProvider != null) {
////            tracerProvider.close();
////        }
////    }
////}
//
//
//
//
//
//package com.itc.funkart.config;
//
//import io.opentelemetry.api.trace.Tracer;
//import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
//import io.opentelemetry.sdk.OpenTelemetrySdk;
//import io.opentelemetry.sdk.resources.Resource;
//import io.opentelemetry.sdk.trace.SdkTracerProvider;
//import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import jakarta.annotation.PreDestroy;
//
//@Configuration
//public class OpenTelemetryConfig {
//
//    private SdkTracerProvider tracerProvider;
//
//    @Bean
//    public Tracer tracer() {
//        // OTLP exporter points to OTEL Collector on host (or Docker network)
//        OtlpGrpcSpanExporter otlpExporter = OtlpGrpcSpanExporter.builder()
//                .setEndpoint("http://18.134.196.21:4317") // Must be a full URL
//                .build();
//
//        Resource resource = Resource.getDefault()
//                .merge(Resource.builder()
//                        .put("service.name", "funkart-order-service")
//                        .build()
//                );
//
//        tracerProvider = SdkTracerProvider.builder()
//                .addSpanProcessor(BatchSpanProcessor.builder(otlpExporter).build())
//                .setResource(resource)
//                .build();
//
//        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
//                .setTracerProvider(tracerProvider)
//                .build();
//
//        return openTelemetry.getTracer("com.itc.funkart.OrderService");
//    }
//
//    @PreDestroy
//    public void shutdown() {
//        if (tracerProvider != null) {
//            tracerProvider.close();
//        }
//    }
//}