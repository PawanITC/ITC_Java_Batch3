package com.itc.funkart.gateway.security;

import com.itc.funkart.common.dto.user.JwtUserDto;
import com.itc.funkart.gateway.service.JwtService;
import com.itc.funkart.gateway.service.TokenBlacklistService;
import lombok.extern.slf4j.Slf4j; // Added for professional logging
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * <h2>JwtAuthWebFilter</h2>
 * <p>
 * Responsible for establishing the "Operating Environment" for each request.
 * It translates external "bits" (JWT Cookies) into internal "Actors" (Authentication).
 * <p>
 * TLDR - Handles JWT extraction, blacklist validation, and SecurityContext population.
 */
@Slf4j
@Component
public class JwtAuthWebFilter implements WebFilter {

    private final CookieUtil cookieUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtService jwtService;
    private final SecurityResponseWriter responseWriter;

    public JwtAuthWebFilter(CookieUtil cookieUtil,
                            TokenBlacklistService tokenBlacklistService,
                            JwtService jwtService,
                            SecurityResponseWriter responseWriter) {
        this.cookieUtil = cookieUtil;
        this.tokenBlacklistService = tokenBlacklistService;
        this.jwtService = jwtService;
        this.responseWriter = responseWriter;
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. Skip logic for observability endpoints
        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        // Also skip logic for observability and public authentication endpoints
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String token = cookieUtil.extractToken(exchange);

        // 2. No token? Pass to security config to handle (permit or deny)
        if (token == null || token.isBlank()) {
            return chain.filter(exchange);
        }

        // 3. Resilience Pattern: Check blacklist with timeout and fallback
        return tokenBlacklistService.isBlacklisted(token)
                .timeout(Duration.ofMillis(500))
                .onErrorResume(e -> {
                    // Critical: Log the error so you know Redis is down!
                    log.error("Resilience Triggered: Redis unreachable. Skipping blacklist check.", e);
                    return Mono.just(false);
                })
                .flatMap(isBlacklisted -> {
                    if (isBlacklisted) {
                        return responseWriter.writeUnauthorized(exchange);
                    }

                    // 4. Transform Token to Principal
                    return Mono.fromCallable(() -> {
                                var claims = jwtService.parseClaims(token);
                                Long userId = jwtService.getUserId(claims);
                                String role = claims.get("role", String.class);

                                var principal = new JwtUserDto(userId, null, null, role);
                                return new UsernamePasswordAuthenticationToken(
                                        principal,
                                        null,
                                        List.of(new SimpleGrantedAuthority(role))
                                );
                            })
                            .flatMap(auth -> chain.filter(exchange)
                                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                            .onErrorResume(Exception.class, e -> {
                                log.warn("JWT validation failed for request: {}", path);
                                return responseWriter.writeUnauthorized(exchange);
                            });
                });
    }


    /**
     * Returns 401 Unauthorized and terminates request.
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    /**
     * Helper method for clarity
     */
    private boolean isPublicPath(String path) {
        return path.startsWith("/actuator") ||
                path.contains("/api/v1/users/login") ||
                path.contains("/api/v1/users/signup") ||
                path.contains("/api/v1/oauth");
    }
}