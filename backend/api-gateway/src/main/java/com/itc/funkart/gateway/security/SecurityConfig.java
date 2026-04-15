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
    private final AppConfig appConfig;


    public SecurityConfig(JwtWebFilter jwtWebFilter, AppConfig appConfig) {
        this.jwtWebFilter = jwtWebFilter;
        this.appConfig = appConfig;
    }

    /**
     * Configures the Security Web Filter Chain for the reactive stack.
     * * @param http The core security builder for WebFlux.
     * @return The finalized security chain.
     */
    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        String version = "/api/v1";
        try {
            if (appConfig.api() != null && appConfig.api().version() != null) {
                version = appConfig.api().version();
            }
        } catch (Exception e) {
            // Fallback for test context initialization
        }

        String finalVersion = version;
        return http
                // Disable CSRF because we use JWTs and our Gateway is stateless
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // Inject our custom JWT 'Re-hydration' logic before the standard Auth check
                .addFilterBefore(jwtWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                .authorizeExchange(auth -> auth
                        // 1. PUBLIC: Infrastructure & OAuth (No Version Prefix)
                        .pathMatchers(
                                "/oauth/github/**",
                                "/health"
                        ).permitAll()

                        // 2. PUBLIC: Auth & Webhooks (With Version Prefix)
                        .pathMatchers(
                                finalVersion + "/users/login",
                                finalVersion + "/users/signup",
                                finalVersion + "/payments/webhook"
                        ).permitAll()

                        // 3. SECURE: Admin-only sections (Example of RBAC)
                        // Note: hasRole checks for "ROLE_ADMIN" if your filter provides "ROLE_ADMIN"
                        .pathMatchers(finalVersion + "/admin/**").hasRole("ADMIN")

                        // 4. SECURE: Everything else requires a valid JWT
                        .anyExchange().authenticated()
                )

                // Disable traditional login forms; the Gateway is a headless API
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .build();
    }
}