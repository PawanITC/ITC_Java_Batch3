package com.itc.funkart.gateway.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * <h2>Central Security Policy for API Gateway</h2>
 *
 * <p>Acts as the primary gatekeeper for the microservices ecosystem.
 * This configuration operates in a Reactive (WebFlux) environment.</p>
 *
 * <p><b>Architecture Rule:</b> The Gateway enforces routing-level authorization.
 * It permits public authentication traffic and validates JWTs for protected
 * resources via the {@link JwtAuthWebFilter}.</p>
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Defines security rules for gateway routes.
     * * @param http The {@link ServerHttpSecurity} builder.
     * @param corsSource Configured CORS policy for frontend access.
     * @param jwtAuthWebFilter Custom filter to validate JWTs in the request flow.
     * @return The configured {@link SecurityWebFilterChain}.
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

                // 2. CORS config (Enables frontend to talk to Gateway)
                .cors(cors -> cors.configurationSource(corsSource))

                // 3. JWT authentication filter
                .addFilterAt(jwtAuthWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                // 4. Authorization rules
                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.OPTIONS).permitAll() // Allow pre-flight requests

                        // PUBLIC ENDPOINTS: No JWT required
                        .pathMatchers(
                                "/api/v1/users/login",
                                "/api/v1/users/signup",
                                "/api/v1/users/oauth/**",   // For your internal user service oauth paths
                                "/api/v1/oauth/**",         // For the direct gateway oauth paths
                                "/api/v1/users/health",
                                "/actuator/",             // Allow health checks
                                "/payments/webhook/**"
                        ).permitAll()

                        // PROTECTED ENDPOINTS: Requires valid JWT (e.g., /api/v1/users/me)
                        .anyExchange().authenticated()
                )
                .build();
    }
}