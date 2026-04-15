package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.AppConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * <h2>Central Security Policy</h2>
 * <p>This class defines the "Rules of the Road" for every request hitting the Gateway.
 * It manages CSRF, Authentication filters, and URL-based authorization.</p>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtWebFilter jwtWebFilter;
    private final String version;


    public SecurityConfig(JwtWebFilter jwtWebFilter, AppConfig appConfig) {
        this.jwtWebFilter = jwtWebFilter;
        this.version = appConfig.api().version();
    }

    /**
     * Configures the Security Web Filter Chain for the reactive stack.
     * * @param http The core security builder for WebFlux.
     * @return The finalized security chain.
     */
    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
         return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .addFilterBefore(jwtWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(auth -> auth
                        .pathMatchers(
                                "/oauth/github/**",
                                "/health",
                                "/payments/webhook"
                        ).permitAll()
                        .pathMatchers(
                                version + "/users/login",
                                version + "/users/signup"
                        ).permitAll()
                        .anyExchange().authenticated()
                )
                .build();
    }
}