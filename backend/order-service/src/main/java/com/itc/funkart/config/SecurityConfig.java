package com.itc.funkart.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtWebFilter jwtWebFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // CORS is handled entirely by the API Gateway — disable it here
                // to prevent a duplicate Access-Control-Allow-Origin header.
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public Endpoints
                        .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()

                        // User & Admin Endpoints — admins must also be able to view/manage orders
                        .requestMatchers("/orders/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")

                        // Admin-only order management endpoints
                        .requestMatchers("/admin/orders/**").hasAuthority("ROLE_ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtWebFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
