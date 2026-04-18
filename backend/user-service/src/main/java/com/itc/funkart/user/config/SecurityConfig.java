package com.itc.funkart.user.config;

import com.itc.funkart.user.auth.JwtWebFilter;
import com.itc.funkart.user.auth.PrincipalFactory;
import com.itc.funkart.user.auth.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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

    /**
     * Creates JWT authentication filter.
     */
    @Bean
    public JwtWebFilter jwtWebFilter() {
        return new JwtWebFilter(jwtService, principalFactory);
    }

    /**
     * Defines stateless security rules.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/users/signup",
                                "/users/login",
                                "/users/oauth/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtWebFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}