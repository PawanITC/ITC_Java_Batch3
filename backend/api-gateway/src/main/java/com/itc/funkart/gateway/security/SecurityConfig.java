package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.props.ApiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * <h2>Central Security Policy</h2>
 * <p>Defines the security rules for the Gateway, including CORS, JWT filtering,
 * and path-based authorization.</p>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final ApiProperties apiProperties;
    private final JwtWebFilter jwtWebFilter;

    public SecurityConfig(JwtWebFilter jwtWebFilter, ApiProperties apiProperties) {
        this.jwtWebFilter = jwtWebFilter;
        this.apiProperties = apiProperties;
    }

    /**
     * Configures the Security Web Filter Chain.
     *
     * @param http       The core security builder.
     * @param corsSource Injected from your CorsConfig class.
     * @return The finalized security chain.
     */
    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http, UrlBasedCorsConfigurationSource corsSource) {
        // Resolve the version prefix (e.g., "/api/v1") to keep matchers in sync with WebConfig
        String v = apiProperties.version();

        return http
                // 1. Activate CORS using your existing CorsConfig source
                .cors(cors -> cors.configurationSource(corsSource))

                // 2. Disable unnecessary defaults for a Stateless API
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // 3. Custom 401 Handling
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, ex2) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                )

                // 4. JWT Filter placement: Re-hydrates user identity before authentication checks
                .addFilterBefore(jwtWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                // 5. Authorization Rules
                .authorizeExchange(auth -> auth
                        // Permit all Preflight OPTIONS requests for CORS compliance
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public Auth & User Endpoints (Dynamic Versioning)
                        .pathMatchers(
                                v + "/users/login",
                                v + "/users/signup",
                                v + "/users/exists/**"
                        ).permitAll()

                        // OAuth2 & Infrastructure (Usually bypass versioning via @NoApiPrefix)
                        .pathMatchers(
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/health",
                                "/actuator/**" // Required for Prometheus/Grafana metrics
                        ).permitAll()

                        // Webhooks and specific service bypasses
                        .pathMatchers(v + "/payments/webhook/**").permitAll()

                        // Everything else requires a valid JWT session
                        .anyExchange().authenticated()
                )
                .build();
    }
}