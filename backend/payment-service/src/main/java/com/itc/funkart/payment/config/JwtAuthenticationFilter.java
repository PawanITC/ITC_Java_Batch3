package com.itc.funkart.payment.config;

import com.itc.funkart.payment.dto.jwt.JwtUserDto;
import com.itc.funkart.payment.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Security Filter for intercepting and validating JWTs.
 * <p>
 * Extends {@link OncePerRequestFilter} to ensure it executes exactly once
 * per incoming HTTP request, even during internal server forwards.
 * </p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        /* * DEV NOTE: The Bearer Pattern
         * Standard APIs use the "Authorization" header with a "Bearer " prefix.
         * We "substring(7)" to strip the word "Bearer " and get just the raw token.
         */
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                if (jwtService.validateToken(token)) {
                    Claims claims = jwtService.parseJwtToken(token);

                    /* * We reconstruct the JwtUserDto from the token's 'Claims'.
                     * This allows our Controllers to know exactly WHO is calling
                     * without having to query the Database again!
                     */
                    JwtUserDto userDto = new JwtUserDto(
                            Long.parseLong(claims.getSubject()),
                            claims.get("name", String.class),
                            claims.get("email", String.class)
                    );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDto,
                                    null,
                                    Collections.emptyList());

                    /* * Setting the SecurityContextHolder "activates" the user.
                     * After this line, Spring Security treats the request as 'Authenticated'.
                     */
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ex) {
                // If a token is expired or fake, we log stacktrace of it and move on.
                // The filterChain will eventually block them at the SecurityConfig level.
                logger.error("Security Filter Error: {}", ex);
            }
        }

        // CRITICAL: Always call doFilter so the request continues to the next step!
        filterChain.doFilter(request, response);
    }
}