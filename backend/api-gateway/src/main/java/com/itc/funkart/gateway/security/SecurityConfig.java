package com.itc.funkart.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Central Security Policy for API Gateway.
 * Acts as the gatekeeper for routing-level authorization only.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Defines security rules for gateway routes.
     * Gateway responsibilities:
     * - Allow public authentication endpoints
     * - Enforce authentication for all other routes
     * - Delegate actual identity verification to JWT filter layer
     */
    @Bean
    public SecurityWebFilterChain apiGatewaySecurityFilterChain(
            ServerHttpSecurity http,
            UrlBasedCorsConfigurationSource corsSource,
            JwtAuthWebFilter jwtAuthWebFilter
    ) {

        return http
                // 1. Stateless gateway (no sessions, no CSRF)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // 2. CORS config (frontend access)
                .cors(cors -> cors.configurationSource(corsSource))

                // 3. JWT authentication filter (IMPORTANT)
                .addFilterAt(jwtAuthWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                // 4. Authorization rules
                .authorizeExchange(auth -> auth

                        // Public auth endpoints
                        .pathMatchers("/users/login").permitAll()
                        .pathMatchers("/users/signup").permitAll()
                        .pathMatchers("/users/oauth/**").permitAll()

                        // Health / infra endpoints (recommended addition)
                        .pathMatchers("/actuator/**").permitAll()

                        // Webhooks (external systems)
                        .pathMatchers("/payments/webhook/**").permitAll()

                        // Preflight
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()

                        // Everything else secured
                        .anyExchange().authenticated()
                )

                .build();
    }
}