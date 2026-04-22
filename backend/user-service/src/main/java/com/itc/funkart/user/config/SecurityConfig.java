package com.itc.funkart.user.config;

import com.itc.funkart.user.auth.JwtService;
import com.itc.funkart.user.auth.JwtAuthWebFilter;
import com.itc.funkart.user.auth.PrincipalFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * <h2>Security Configuration</h2>
 * * <p>Stateless JWT-based security configuration for the User Service.</p>
 * * <p>This configuration enforces that all requests are authenticated via a JSON Web Token (JWT)
 * except for specified public endpoints. It ensures no HTTP Session is created or used
 * for authentication.</p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final PrincipalFactory principalFactory;

    /**
     * Configures the custom JWT filter to intercept and validate tokens in the request header/cookies.
     * @return an instance of {@link JwtAuthWebFilter}
     */
    @Bean
    public JwtAuthWebFilter jwtWebFilter() {
        return new JwtAuthWebFilter(jwtService, principalFactory);
    }

    /**
     * Bypasses the Spring Security Filter Chain for infrastructure and health endpoints.
     * This improves performance for high-frequency monitoring requests.
     * @return a {@link WebSecurityCustomizer} with ignoring rules.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/users/health")
                .requestMatchers("/actuator/**");
    }

    /**
     * Defines the security constraints for HTTP requests.
     * * @param http the {@link HttpSecurity} to modify
     * @return the built {@link SecurityFilterChain}
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public Auth & OAuth Endpoints
                        .requestMatchers(
                                "/users/login",
                                "/users/signup",
                                "/users/oauth/**"
                        ).permitAll()

                        // All other resources (including /users/me) require JWT
                        .anyRequest().authenticated()
                )
                // Intercept requests before they reach the controller
                .addFilterBefore(jwtWebFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Prevents Spring Boot from generating a default security password in the logs.
     * Notifies Spring that identity management is handled via JWT.
     * @return a no-op {@link UserDetailsService}
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Authentication is JWT-based, not database-based.");
        };
    }
}