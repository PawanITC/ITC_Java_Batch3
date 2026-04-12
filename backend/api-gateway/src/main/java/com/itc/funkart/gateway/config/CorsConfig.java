package com.itc.funkart.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(AppConfig appConfig) {
        CorsConfiguration config = new CorsConfiguration();

        // Allowed origins (frontend + localhost for dev)
        String frontend = appConfig.frontendUrl();
        if (frontend != null && !frontend.isBlank()) {
            config.addAllowedOrigin(frontend);
        }
        config.addAllowedOrigin("http://localhost:3000");

        // Allow credentials (cookies, auth headers)
        config.setAllowCredentials(true);

        // Allowed HTTP methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allowed request headers
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));

        // Exposed headers in response
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));

        // Max age for preflight cache
        config.setMaxAge(3600L);

        // Apply config to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}