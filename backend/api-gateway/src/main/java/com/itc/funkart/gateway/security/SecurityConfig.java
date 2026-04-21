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

                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // 2. CORS config (frontend access)
                .cors(cors -> cors.configurationSource(corsSource))

                // 3. JWT authentication filter (IMPORTANT)
                .addFilterAt(jwtAuthWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                // 4. Authorization rules
                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        // ONLY these specific endpoints are public
                        .pathMatchers(
                                "/api/v1/users/login",
                                "/api/v1/users/signup",
                                "/api/v1/users/refresh",
                                "/api/v1/oauth/github/login",    // Initial redirect trigger
                                "/api/v1/oauth/github/callback", // GitHub's return journey
                                "/api/v1/users/health",
                                "/api/v1/oauth/github/refresh",
                                "/payments/webhook/**"          // OAuth token rotation
                        ).permitAll()

                        // Health / infra endpoints
                        .pathMatchers("/actuator/**").permitAll()

                        // Everything else (including /api/v1/users/profile) requires a token
                        .anyExchange().authenticated()
                )

                .build();
    }
}