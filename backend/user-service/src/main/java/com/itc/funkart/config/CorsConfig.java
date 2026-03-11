package com.itc.funkart.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${frontend.url}")
    private String frontendUrl; // fallback to localhost for dev

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Allowed origins (frontend + localhost for dev)
        config.setAllowedOrigins(List.of(frontendUrl, "http://localhost:3000"));
        // Allow credentials (cookies, auth headers)
        config.setAllowCredentials(true);
        // Allowed HTTP methods
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        // Allowed request headers
        config.setAllowedHeaders(List.of("Authorization","Content-Type","Accept","Origin","X-Requested-With"));
        // Exposed headers in response
        config.setExposedHeaders(List.of("Authorization","Content-Type"));
        // Apply config to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}