package com.itc.funkart.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
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

    @Bean
    public CorsWebFilter corsWebFilter(AppConfig appConfig) {
        CorsConfiguration config = new CorsConfiguration();

        // 1. Trust the Frontend URL from config
        if (appConfig.frontendUrl() != null) {
            config.addAllowedOrigin(appConfig.frontendUrl());
        }

        // 2. Trust localhost for developer productivity
        config.addAllowedOrigin("http://localhost:3000");

        // 3. Essential for JWT cookies! Without this, the browser won't send the 'token' cookie.
        config.setAllowCredentials(true);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}