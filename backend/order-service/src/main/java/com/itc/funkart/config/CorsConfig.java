package com.itc.funkart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Global Cross-Origin Resource Sharing (CORS) Configuration.
 * <p>
 * This class provides a dual-layer CORS configuration:
 * 1. <b>Spring Security Layer:</b> Via {@link CorsConfigurationSource}, ensuring pre-flight
 * OPTIONS requests are handled before reaching security filters.
 * 2. <b>Spring MVC Layer:</b> Via {@link WebMvcConfigurer}, providing a fallback for
 * standard controller mapping.
 * </p>
 * <p>
 * Configuring CORS here allows for the removal of {@code @CrossOrigin} annotations
 * at the controller level, promoting a centralized security policy.
 * </p>
 */
// @Configuration — DISABLED: this service sits behind the API Gateway which owns CORS.
// Re-enabling this would add a second Access-Control-Allow-Origin header and
// cause browsers to reject all cross-origin responses with a "multiple values" error.
public class CorsConfig implements WebMvcConfigurer {

    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:5173",      // Frontend dev server (Vite)
            "http://localhost:9090",      // Local API (Swagger/Gateway)
            "http://localhost:8060",
            "http://funkart.local",       // Local custom domain
            "https://funkart.com"         // Production domain
    );

    private static final List<String> ALLOWED_METHODS = List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
    );

    /**
     * Standard MVC CORS mapping.
     * Used by Spring WebMVC to handle cross-origin requests at the dispatcher level.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(ALLOWED_ORIGINS.toArray(new String[0]))
                .allowedMethods(ALLOWED_METHODS.toArray(new String[0]))
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * Security-compliant CORS configuration source.
     * <p>
     * Critically important when using Spring Security, as it ensures CORS headers
     * are processed during the security filter chain execution.
     * </p>
     *
     * @return A configured {@link CorsConfigurationSource} for the Security Filter Chain.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(ALLOWED_ORIGINS);
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
