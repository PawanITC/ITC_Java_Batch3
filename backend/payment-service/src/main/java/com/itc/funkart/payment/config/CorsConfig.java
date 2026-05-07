package com.itc.funkart.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // Servlet version

import java.util.List;

// @Configuration — DISABLED: this service sits behind the API Gateway which owns CORS.
// Re-enabling this would add a second Access-Control-Allow-Origin header and
// cause browsers to reject all cross-origin responses with a "multiple values" error.
public class CorsConfig {

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 1. Match your Gateway settings
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:8060" // Allow the Gateway's origin if needed
        ));

        config.setAllowCredentials(true);
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        config.setMaxAge(3600L);

        // 2. Use the Servlet-based Source
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
