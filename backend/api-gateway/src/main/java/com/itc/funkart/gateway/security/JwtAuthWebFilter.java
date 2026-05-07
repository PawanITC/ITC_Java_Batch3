package com.itc.funkart.gateway.security;

import com.itc.funkart.common.dto.user.JwtUserDto;
import com.itc.funkart.gateway.service.JwtService;
import com.itc.funkart.gateway.service.TokenBlacklistService;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Component
public class JwtAuthWebFilter implements WebFilter {

    private final CookieUtil cookieUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtService jwtService;
    private final SecurityResponseWriter responseWriter;

    public JwtAuthWebFilter(
            CookieUtil cookieUtil,
            TokenBlacklistService tokenBlacklistService,
            JwtService jwtService,
            SecurityResponseWriter responseWriter
    ) {
        this.cookieUtil = cookieUtil;
        this.tokenBlacklistService = tokenBlacklistService;
        this.jwtService = jwtService;
        this.responseWriter = responseWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // =========================================================
        // 1. BYPASS ALL PUBLIC ROUTES (CRITICAL FOR OAUTH)
        // =========================================================
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // =========================================================
        // 2. EXTRACT TOKEN ONLY FOR PROTECTED ROUTES
        // =========================================================
        String token = cookieUtil.extractToken(exchange);

        if (token == null || token.isBlank()) {
            return chain.filter(exchange);
        }

        // =========================================================
        // 3. BLACKLIST CHECK (RESILIENT)
        // =========================================================
        return tokenBlacklistService.isBlacklisted(token)
                .timeout(Duration.ofMillis(500))
                .onErrorReturn(false)
                .flatMap(isBlacklisted -> {

                    if (isBlacklisted) {
                        return responseWriter.writeUnauthorized(exchange);
                    }

                    // =========================================================
                    // 4. JWT PARSE + SECURITY CONTEXT
                    // =========================================================
                    try {
                        var claims = jwtService.parseClaims(token);

                        Long userId = jwtService.getUserId(claims);
                        String role = claims.get("role", String.class);

                        var principal = new JwtUserDto(userId, null, null, role);

                        var auth = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority(role))
                        );

                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

                    } catch (Exception e) {
                        log.warn("JWT invalid for {}: {}", path, e.getMessage());
                        return responseWriter.writeUnauthorized(exchange);
                    }
                });
    }

    /**
     * PUBLIC ROUTES — MUST INCLUDE ENTIRE OAUTH FLOW PREFIXES ONLY
     */
    private boolean isPublicPath(String path) {
        return path.startsWith("/actuator")

                // AUTH
                || path.startsWith("/api/v1/users/login")
                || path.startsWith("/api/v1/users/signup")
                || path.startsWith("/api/v1/users/logout")

                // OAUTH (CRITICAL FIX — CLEANED UP)
                || path.startsWith("/api/v1/oauth")
                || path.startsWith("/api/v1/users/oauth")

                // PAYMENTS WEBHOOKS
                || path.startsWith("/api/v1/payments/webhook");
    }
}