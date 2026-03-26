package com.itc.funkart.payment.config;

import com.itc.funkart.payment.security.JwtWebFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtWebFilter jwtWebFilter;

    public SecurityConfig(JwtWebFilter jwtWebFilter) {
        this.jwtWebFilter = jwtWebFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtWebFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(
                                "/api/v1/users/signup",
                                "/api/v1/users/login",
                                "/api/v1/users/oauth/github",
                                "/api/v1/payments/webhook",  // ← ADD WEBHOOK
                                "/actuator/**"
                        )
                        .permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> {
                });

        return http.build();
    }
}