package com.itc.funkart.product_service.config;

import lombok.RequiredArgsConstructor;
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
 * Security Blueprint for the Product Service.
 * <p>
 * Configures the {@link SecurityFilterChain} to define access boundaries for
 * the e-commerce catalog and cart management. This service operates in a
 * stateless manner, relying on JWTs for identity verification.
 * </p>
 *
 * <h2>Access Control Matrix:</h2>
 * <table border="1">
 * <caption>Endpoint Authorization Rules</caption>
 * <tr>
 * <th>Path Pattern</th>
 * <th>HTTP Method</th>
 * <th>Authority Required</th>
 * <th>Purpose</th>
 * </tr>
 * <tr>
 * <td>/products/**, /categories/**</td>
 * <td>GET</td>
 * <td>PERMIT_ALL</td>
 * <td>Public browsing of the product catalog.</td>
 * </tr>
 * <tr>
 * <td>/products/by-ids</td>
 * <td>POST</td>
 * <td>PERMIT_ALL</td>
 * <td>Batch retrieval for cart/checkout summaries.</td>
 * </tr>
 * <tr>
 * <td>/cart/**</td>
 * <td>ANY</td>
 * <td>ROLE_USER</td>
 * <td>Private shopping cart operations for consumers.</td>
 * </tr>
 * <tr>
 * <td>/admin/**</td>
 * <td>ANY</td>
 * <td>ROLE_ADMIN</td>
 * <td>Catalog maintenance and inventory management.</td>
 * </tr>
 * </table>
 *
 * <p>
 * Key Security Features:
 * <ul>
 * <li><b>CSRF Disabled:</b> Appropriate for stateless REST APIs using Bearer tokens.</li>
 * <li><b>CORS Integration:</b> Leverages {@link CorsConfig#corsConfigurationSource()}
 * to handle pre-flight requests.</li>
 * <li><b>Stateless Sessions:</b> No HTTP sessions are created or used.</li>
 * <li><b>JWT Filter:</b> Injects {@link JwtWebFilter} before the standard
 * authentication filter.</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtWebFilter jwtWebFilter;
    private final CorsConfig corsConfig;

    /**
     * Defines the primary security filter chain bean.
     *
     * @param http The {@link HttpSecurity} builder.
     * @return The configured {@link SecurityFilterChain}.
     * @throws Exception If an error occurs during the security setup.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // Use the configuration source from our centralized CorsConfig
                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. Static Resources & OpenAPI Docs
                        .requestMatchers(
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // 2. Public Catalog (Browsing is unrestricted)
                        .requestMatchers(HttpMethod.GET, "/products/**", "/categories/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/products/by-ids").permitAll()

                        // 3. Shopping Cart (Restricted to authenticated Users)
                        .requestMatchers("/cart/**").hasAuthority("ROLE_USER")

                        // 4. Admin Management (Restricted to authenticated Admins)
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")

                        // 5. Catch-all for any other endpoint
                        .anyRequest().authenticated()
                )
                // Inject our custom JWT validation logic into the filter chain
                .addFilterBefore(jwtWebFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}