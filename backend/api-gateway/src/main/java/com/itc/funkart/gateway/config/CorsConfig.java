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

        // IMPORTANT: credentials ON
        config.setAllowCredentials(true);

        // EXACT origins only (no wildcards)
        config.setAllowedOrigins(List.of(
                appConfig.frontendUrl(),
                "http://localhost:5173",
                "http://localhost:3000"
        ));

        // STRICT headers (not *)
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));

        // STRICT methods
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));

        config.setExposedHeaders(List.of(
                "Authorization",
                "Set-Cookie"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}