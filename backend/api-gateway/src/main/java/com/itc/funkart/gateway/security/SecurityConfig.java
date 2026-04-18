package com.itc.funkart.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
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
            UrlBasedCorsConfigurationSource corsSource
    ) {

        return http
                // Stateless gateway (no sessions, no CSRF)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // CORS delegated to central configuration
                .cors(cors -> cors.configurationSource(corsSource))

                // Allow unauthenticated requests where required
                .anonymous(Customizer.withDefaults())

                .authorizeExchange(auth -> auth
                        // Auth flows (public endpoints)
                        .pathMatchers("/users/login").permitAll()
                        .pathMatchers("/users/signup").permitAll()
                        .pathMatchers("/users/oauth/**").permitAll()

                        // External webhook access
                        .pathMatchers("/payments/webhook/**").permitAll()

                        // Preflight requests
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()

                        // Everything else requires valid JWT
                        .anyExchange().authenticated()
                )

                .build();
    }
}