package com.itc.funkart.payment.config;

import com.itc.funkart.payment.security.JwtWebFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security Blueprint for the Payment Service.
 * <p>
 * This configuration handles two distinct traffic types:
 * 1. Public Webhooks: Secured via Stripe Signature (handled in Controller).
 * 2. Private API: Secured via JWT Bearer tokens.
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtWebFilter jwtWebFilter;
    private final ApiConfig apiConfig;

    public SecurityConfig(JwtWebFilter jwtWebFilter, ApiConfig apiConfig) {
        this.jwtWebFilter = jwtWebFilter;
        this.apiConfig = apiConfig;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String baseApi = "/" + apiConfig.getVersion();

        http
                // 1. Disable CSRF because we are a Stateless REST API
                // (Stripe Webhooks would fail if this was enabled)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Set Session Management to Stateless (Standard for JWT)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 3. Routing Permissions
                .authorizeHttpRequests(auth -> auth
                        // Public: The "Back Door" for Stripe events and Health Checks
                        .requestMatchers(baseApi + "/payments/webhook", "/actuator/**")
                        .permitAll()
                        // Private: Everything else requires a valid JWT
                        .anyRequest().authenticated()
                )

                // 4. Custom Filters
                // Insert our JWT logic before the standard Username/Password check
                .addFilterBefore(jwtWebFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}