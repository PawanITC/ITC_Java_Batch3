package com.itc.funkart.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * <h2>Cross-Origin Resource Sharing (CORS) Configuration</h2>
 * <p>Since our Frontend (Port 3000) and Gateway (Port 8080) live on different "origins,"
 * the browser will block requests by default. This filter adds the headers needed to
 * allow secure communication.</p>
 */
@Configuration
public class CorsConfig {

    // 1. This is the bean SecurityConfig is looking for!
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource(AppConfig appConfig) {
        CorsConfiguration config = new CorsConfiguration();

        // Ensure we allow the frontend
        config.setAllowedOrigins(List.of(
                appConfig.frontendUrl(),
                "http://localhost:5173"
        ));

        config.setAllowCredentials(true);
        // Using "*" for headers during testing helps rule out header-mismatch 403s
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}