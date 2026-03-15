package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.dto.JwtUserDto;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

/**
 * WebFilter for JWT token validation and extraction.
 * Extracts token from Authorization header or cookie,
 * validates it, and sets up security context for downstream handlers.
 * Does NOT generate tokens (that's user-service's job).
 */
@Component
public class JwtWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtWebFilter.class);

    private final CookieUtil cookieUtil;
    private final JwtTokenValidator jwtTokenValidator;

    public JwtWebFilter(CookieUtil cookieUtil, JwtTokenValidator jwtTokenValidator) {
        this.cookieUtil = cookieUtil;
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String token = extractToken(exchange);

        if (token != null && !token.isBlank()) {
            try {
                // Validate and parse JWT claims
                Claims claims = jwtTokenValidator.validateAndParseClaims(token);

                // Build user from JWT claims (no DB needed)
                Long userId = Long.parseLong(claims.getSubject());
                String name = (String) claims.get("name");
                String email = (String) claims.get("email");

                JwtUserDto user = new JwtUserDto(userId, name, email);

                // Create authentication token
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());

                // Continue with authentication in reactive context
                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

            } catch (io.jsonwebtoken.ExpiredJwtException ex) {
                log.warn("JWT token expired: {}", ex.getMessage());
                return chain.filter(exchange);
            } catch (Exception ex) {
                log.warn("JWT token invalid: {}", ex.getMessage());
                return chain.filter(exchange);
            }
        }

        return chain.filter(exchange);
    }

    /**
     * Extract token from Authorization header or cookie
     */
    private String extractToken(ServerWebExchange exchange) {
        // Check Authorization header
        List<String> authHeaders = exchange.getRequest().getHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String header = authHeaders.get(0);
            if (header.startsWith("Bearer ")) {
                log.debug("JWT token found in Authorization header");
                return header.substring(7);
            }
        }

        // Check cookies
        String cookieName = cookieUtil.getCookieName();
        if (exchange.getRequest().getCookies().containsKey(cookieName)) {
            var cookies = exchange.getRequest().getCookies().get(cookieName);
            if (!cookies.isEmpty()) {
                log.debug("JWT token found in cookie");
                return cookies.get(0).getValue();
            }
        }

        log.debug("No JWT token found");
        return null;
    }
}