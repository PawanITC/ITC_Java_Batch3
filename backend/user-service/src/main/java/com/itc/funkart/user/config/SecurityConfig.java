package com.itc.funkart.user.config;

import com.itc.funkart.user.security.JwtWebFilter;
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
 * Main security policy configuration for the User Service.
 * <p>This class defines the {@link SecurityFilterChain} which dictates the
 * authentication and authorization rules for all incoming HTTP requests.</p>
 *
 * <p><b>Design Choice:</b> We utilize Constructor Injection for all dependencies
 * (via {@link RequiredArgsConstructor}) to ensure the Security Context is immutable
 * and fully initialized at startup, preventing NullPointerExceptions during
 * filter registration.</p>
 *
 * @author Gemini
 * @version 1.5
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Configuration properties bean containing API versioning metadata. */
    private final ApiConfig apiConfig;

    /** Custom JWT authentication filter. */
    private final JwtWebFilter jwtWebFilter;

    /**
     * Configures the HTTP security filter chain.
     * <ul>
     * <li><b>CSRF:</b> Disabled as the service utilizes stateless JWTs.</li>
     * <li><b>Session:</b> Set to {@link SessionCreationPolicy#STATELESS}.</li>
     * <li><b>Authorization:</b> Permits public access to onboarding (signup/login)
     * and OAuth flows, while requiring authentication for the rest of the API.</li>
     * <li><b>Filters:</b> Injects {@code JwtWebFilter} ahead of the
     * {@link UsernamePasswordAuthenticationFilter}.</li>
     * </ul>
     *
     * @param http The {@link HttpSecurity} builder.
     * @return The fully configured {@link SecurityFilterChain}.
     * @throws Exception if an error occurs during the configuration build phase.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // Build the prefix string (e.g., "/api/v1") to align with WebConfig path mapping
        final String prefix = "/" + apiConfig.getVersion();

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                prefix + "/users/signup",
                                prefix + "/users/login",
                                prefix + "/users/oauth/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtWebFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}