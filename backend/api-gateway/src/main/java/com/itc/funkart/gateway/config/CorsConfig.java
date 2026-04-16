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

        // ---------------------------
        // Origins (single source)
        // ---------------------------
        String frontend = appConfig.frontendUrl();

        if (frontend != null && !frontend.isBlank()) {
            config.addAllowedOrigin(frontend);
        }

        // Dev fallback ONLY (keep minimal and explicit)
        config.addAllowedOrigin("http://localhost:5173");

        // If you still need legacy support, keep ONE:
        // config.addAllowedOrigin("http://localhost:3000");

        // ---------------------------
        // Critical for JWT cookies
        // ---------------------------
        config.setAllowCredentials(true);

        // ---------------------------
        // Headers (be explicit)
        // ---------------------------
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));

        // ---------------------------
        // Methods (explicit is safer than "*")
        // ---------------------------
        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
        ));

        // ---------------------------
        // Exposed headers (frontend-readable only)
        // ---------------------------
        config.setExposedHeaders(List.of(
                "Authorization",
                "Set-Cookie"
        ));

        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}