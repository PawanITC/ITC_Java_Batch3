package com.itc.funkart.gateway.security;

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

    private final JwtWebFilter jwtWebFilter;

    public SecurityConfig(JwtWebFilter jwtWebFilter) {
        this.jwtWebFilter = jwtWebFilter;
    }

    /**
     * Configures the Security Web Filter Chain.
     * * @param http The core security builder.
     * @param corsSource Injected from your CorsConfig class.
     * @return The finalized security chain.
     */
    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http, UrlBasedCorsConfigurationSource corsSource) {
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

                // 4. JWT Filter placement
                .addFilterBefore(jwtWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                // 5. Authorization Rules
                .authorizeExchange(auth -> auth
                        // Permit all Preflight OPTIONS requests
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public Endpoints
                        .pathMatchers(
                                "/api/v1/users/login",
                                "/api/v1/users/signup",
                                "/oauth/github/**",
                                "/health",
                                "/payments/webhook"
                        ).permitAll()

                        // Everything else requires valid JWT
                        .anyExchange().authenticated()
                )
                .build();
    }
}