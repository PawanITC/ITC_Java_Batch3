package com.itc.funkart.user.config;

import com.itc.funkart.user.auth.JwtService;
import com.itc.funkart.user.auth.JwtWebFilter;
import com.itc.funkart.user.auth.PrincipalFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * <h2>Security Configuration</h2>
 *
 * <p>
 * Stateless JWT-based security configuration for the User Service.
 * </p>
 *
 * <p>
 * <b>Architecture Rule:</b>
 * Authentication is fully handled inside user-service.
 * Gateway does not participate in token validation or identity resolution.
 * </p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final PrincipalFactory principalFactory;

    @Bean
    public JwtWebFilter jwtWebFilter() {
        return new JwtWebFilter(jwtService, principalFactory);
    }

    /**
     * STAGE 1: Bypasses the filter chain entirely.
     * Use this for infra/health check endpoints.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/users/health")
                .requestMatchers("/actuator/**");
    }

    /**
     * STAGE 2: The actual security rules.
     * Only requests NOT ignored by Stage 1 reach here.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public Auth Endpoints
                        .requestMatchers(
                                "/users/login",
                                "/users/signup"
                        ).permitAll()

                        // Everything else requires a valid JWT
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtWebFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}