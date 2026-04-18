package com.itc.funkart.user.auth;

import com.itc.funkart.user.dto.security.UserPrincipalDto;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * <h2>JWT Authentication Filter</h2>
 *
 * <p>
 * Stateless authentication filter responsible only for:
 * <ul>
 *   <li>Token extraction</li>
 *   <li>Delegating validation to JwtService</li>
 *   <li>Delegating identity reconstruction to PrincipalFactory</li>
 *   <li>Populating Spring SecurityContext</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Architecture Rule:</b>
 * This filter must remain stateless and contain ZERO business logic.
 * It is safe to keep it in user-service even after gateway removal.
 * </p>
 */
@Slf4j
public class JwtWebFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final PrincipalFactory principalFactory;

    public JwtWebFilter(JwtService jwtService, PrincipalFactory principalFactory) {
        this.jwtService = jwtService;
        this.principalFactory = principalFactory;
    }

    /**
     * Core authentication pipeline.
     */
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

                UserPrincipalDto principal = principalFactory.fromClaims(claims);

                if (principal == null) {
                    log.warn("JWT parsed but principal is null for {}", request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority(principal.role()))
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception ex) {
                log.warn("JWT authentication failed for {}: {}",
                        request.getRequestURI(),
                        ex.getMessage()
                );
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT from Authorization header or cookie.
     */
    private String extractToken(HttpServletRequest request) {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

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