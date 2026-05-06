package com.itc.funkart.payment.config;

import com.itc.funkart.payment.security.JwtWebFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
    private final CorsConfig corsConfig;

    public SecurityConfig(JwtWebFilter jwtWebFilter, CorsConfig corsConfig) {
        this.jwtWebFilter = jwtWebFilter;
        this.corsConfig = corsConfig;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // Use the injected corsConfig bean method
                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // ADD the /api/v1 prefixed path
                        .requestMatchers("/payments/webhook/**").permitAll()
                        .requestMatchers("/api/v1/payments/webhook/**").permitAll()  // ADD THIS
                        .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/payments/**").hasAuthority("ROLE_USER")
                        .requestMatchers("/api/v1/payments/**").hasAuthority("ROLE_USER")  // ADD THIS
                        .anyRequest().authenticated()
                )
                // Explicitly place JWT filter
                .addFilterBefore(jwtWebFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}