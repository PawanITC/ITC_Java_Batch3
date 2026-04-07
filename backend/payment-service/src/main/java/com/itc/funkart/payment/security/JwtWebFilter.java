package com.itc.funkart.payment.security;

import com.itc.funkart.payment.dto.jwt.JwtUserDto;
import com.itc.funkart.payment.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT interceptor for Servlet-based traffic.
 * <p>
 * This filter extracts the identity of the user from either the "Authorization" header
 * or a "token" cookie. If valid, it populates the SecurityContext with a {@link JwtUserDto}.
 * </p>
 */
@Component
public class JwtWebFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtWebFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && !token.isBlank()) {
            try {
                // 1. Validate and Parse
                Claims claims = jwtService.parseJwtToken(token);

                // 2. Map Claims to our Internal DTO
                JwtUserDto user = new JwtUserDto(
                        Long.parseLong(claims.getSubject()),
                        (String) claims.get("name"),
                        (String) claims.get("email")
                );

                // 3. Set Authentication with "ROLE_USER" (Best Practice)
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        user, null, Collections.emptyList());

                SecurityContextHolder.getContext().setAuthentication(auth);

                // Optional: Trace log for debugging
                logger.trace("Authenticated User: " + user.email());

            } catch (Exception ex) {
                // We don't block the chain here; the SecurityConfig will block
                // unauthorized requests later if the context is empty.
                logger.debug("JWT validation failed: " + ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        // Standard Header Check
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        // Cookie Fallback (Crucial for browser-based Vite apps)
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