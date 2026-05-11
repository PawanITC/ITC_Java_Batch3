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
 * <p>Acts as the primary gatekeeper for the microservices' ecosystem.
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
     *
     * @param corsSource       Configured CORS policy for frontend access.
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
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(cors -> cors.configurationSource(corsSource))

                // Custom JWT validation filter placed in the Authentication slot
                .addFilterAt(jwtAuthWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()

                        /*
                         * OBSERVABILITY & INFRASTRUCTURE:
                         * Permit all Actuator endpoints. In a production EKS environment,
                         * access to these should be restricted at the Network (Security Group) level,
                         * but they must remain accessible to the Prometheus scraper and K8s Probes.
                         */
                        .pathMatchers("/actuator/**").permitAll()

                        // PUBLIC BUSINESS ENDPOINTS
                        .pathMatchers(
                                "/api/v1/users/login",
                                "/api/v1/users/signup",
                                "/api/v1/users/logout",
                                "/api/v1/users/oauth/**",
                                "/api/v1/oauth/**",
                                "/api/v1/payments/webhook/**"
                        ).permitAll()

                        // PROTECTED DOMAIN
                        .anyExchange().authenticated()
                )
                .build();
    }
}