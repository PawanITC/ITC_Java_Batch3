package com.itc.funkart.user.security;

import com.itc.funkart.user.dto.user.JwtUserDto;
import com.itc.funkart.user.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Servlet filter responsible for intercepting HTTP requests and authenticating users
 * via JSON Web Tokens.
 * * <p>The filter attempts to extract a token from:
 * <ol>
 * <li>The {@code Authorization} Bearer header</li>
 * <li>A {@code token} HTTP cookie</li>
 * </ol>
 */
@Slf4j
@Component
public class JwtWebFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtWebFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && !token.isBlank()) {
            try {
                Claims claims = jwtService.parseJwtToken(token);

                String subject = claims.getSubject();
                String name = claims.get("name", String.class);
                String email = claims.get("email", String.class);

                // Defensive check: If the token is "valid" but empty, don't authenticate
                if (subject != null && !subject.isBlank()) {
                    JwtUserDto user = new JwtUserDto(
                            Long.parseLong(subject),
                            name != null ? name : "Unknown User",
                            email != null ? email : ""
                    );

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }

            } catch (Exception ex) {
                // This catches parsing errors, expiration, AND our Long.parseLong errors
                log.warn("JWT validation failed for request {}: {}", request.getRequestURI(), ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Helper to retrieve the token from the request.
     * * @param request The incoming {@link HttpServletRequest}.
     * @return The token string if found, otherwise {@code null}.
     */
    private String extractToken(HttpServletRequest request) {
        // 1. Check Authorization header
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        // 2. Check cookies (useful for browser-based testing/frontend)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}