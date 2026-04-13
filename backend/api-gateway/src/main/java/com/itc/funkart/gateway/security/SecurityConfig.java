package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.AppConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Main security configuration for the API Gateway.
 * Defines the security filter chain, path permissions, and integrates
 * the custom JWT web filter.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtWebFilter jwtWebFilter;
    private final AppConfig appConfig;

    public SecurityConfig(JwtWebFilter jwtWebFilter, AppConfig appConfig) {
        this.jwtWebFilter = jwtWebFilter;
        this.appConfig = appConfig;
    }

    /**
     * Configures the security behavior for the reactive web stack.
     * * @param http The security builder
     * @return The configured security chain
     */
    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        String api = appConfig.api().version(); // "/api/v1"

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .addFilterBefore(jwtWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(auth -> auth
                        // 1. GitHub is infrastructure - NO prefix needed
                        .pathMatchers(
                                "/oauth/github/login",
                                "/oauth/github/callback",
                                "/oauth/github/logout"
                        ).permitAll()

                        // 2. Data Services - Prefix needed
                        .pathMatchers(
                                api + "/users/login",
                                api + "/users/signup",
                                api + "/payments/webhook"
                        ).permitAll()

                        .anyExchange().authenticated()
                )
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .build();
    }
}