package com.itc.funkart.user.config;

import com.itc.funkart.user.auth.JwtAuthWebFilter;
import com.itc.funkart.user.auth.JwtService;
import com.itc.funkart.user.auth.PrincipalFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * <h2>User Service Security Policy</h2>
 *
 * <p>Enforces a stateless security model for the Funkart User Service.
 * This configuration relies on the API Gateway for initial request scrubbing
 * and handles localized authorization based on the paths received after
 * Gateway prefix stripping.</p>
 *
 * <p><b>Key Responsibilities:</b></p>
 * <ul>
 *     <li>Disabling CSRF and Session state for REST compatibility.</li>
 *     <li>Permitting unauthenticated access to login and registration routes.</li>
 *     <li>Injecting the {@link JwtAuthWebFilter} into the standard FilterChain.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final PrincipalFactory principalFactory;

    /**
     * Initializes the JWT Filter.
     * <p>Note: Ensure this filter contains logic to call {@code chain.doFilter}
     * even if a token is missing, allowing the {@code permitAll()} paths to proceed.</p>
     *
     * @return The functional {@link JwtAuthWebFilter}.
     */
    @Bean
    public JwtAuthWebFilter jwtWebFilter() {
        return new JwtAuthWebFilter(jwtService, principalFactory);
    }

    /**
     * Configures the main security filter chain for the User Service.
     *
     * <p><b>Note:</b> Paths are adjusted to account for the Gateway's
     * {@code StripPrefix=2} filter. Therefore, {@code /api/v1/users/login}
     * is matched here as {@code /login}.</p>
     *
     * @param http The security builder.
     * @return The configured {@link SecurityFilterChain}.
     * @throws Exception If configuration fails.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Paths are relative to the service after Gateway stripping
                        .requestMatchers("/users/login", "/users/signup", "/users/health").permitAll()
                        .requestMatchers("/oauth/**").permitAll()
                        .requestMatchers("/actuator/**", "/users/actuator/**").permitAll()// Admin routes and other business logic require valid JWT
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // Filter placement is crucial for JWT-based auth
                .addFilterBefore(jwtWebFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Disables the default Spring Security password generation.
     * <p>Explicitly signals that the User Service does not use traditional
     * DAO-based username/password authentication for every request,
     * but rather consumes the identity established via JWT.</p>
     *
     * @return A No-Op {@link UserDetailsService}.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Identity management is managed via JWT Gateway filter.");
        };
    }
}