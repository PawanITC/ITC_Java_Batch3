package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.ApiConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtWebFilter jwtWebFilter;
    private final ApiConfig apiConfig;

    public SecurityConfig(JwtWebFilter jwtWebFilter, ApiConfig apiConfig) {
        this.jwtWebFilter = jwtWebFilter;
        this.apiConfig = apiConfig;
    }

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        String api = apiConfig.getVersion();

        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .addFilterBefore(jwtWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(auth -> auth
                        // Public endpoints - no authentication required
                        .pathMatchers(
                                "/oauth/github/login",
                                "/oauth/github/callback",
                                "/oauth/github/logout",
                                           api + "/users/login",
                                api + "/users/signup",
                                api + "/payments/webhook"
                        ).permitAll()
                        // All other requests require authentication
                        .anyExchange().authenticated()
                )
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable);

        return http.build();
    }
}