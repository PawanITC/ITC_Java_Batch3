package com.itc.funkart.product_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS Configuration for the Product Service
 * 
 * Enables Cross-Origin Resource Sharing (CORS) for:
 * - Swagger UI testing
 * - Frontend applications (localhost:5173)
 * - Other microservices
 * - Production domains
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // Allow requests from these origins
                .allowedOrigins(
                    "http://localhost:5173",      // Frontend dev server
                    "http://localhost:3000",      // Alternative frontend port
                    "http://localhost:9090",      // Local API (for Swagger UI)
                    "http://127.0.0.1:9090",      // Localhost alternative
                    "http://localhost:8080",      // Alternative app port
                    "http://localhost:4200",      // Angular dev server
                    "http://funkart.local",       // Local domain
                    "https://funkart.com"         // Production domain (add your actual domain)
                )
                // Allow these HTTP methods
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD")
                // Allow these headers
                .allowedHeaders("*")
                // Allow credentials (cookies, authorization headers)
                .allowCredentials(true)
                // How long preflight response can be cached (in seconds)
                .maxAge(3600);
    }
}

