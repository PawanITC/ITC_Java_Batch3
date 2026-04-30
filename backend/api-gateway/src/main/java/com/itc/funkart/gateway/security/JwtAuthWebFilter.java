package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.exception.JwtAuthenticationException;
import com.itc.funkart.gateway.service.JwtService;
import com.itc.funkart.gateway.service.TokenBlacklistService;
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

import java.util.List;

@Component
public class JwtAuthWebFilter implements WebFilter {

    private final CookieUtil cookieUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtService jwtService;
    private final SecurityResponseWriter responseWriter;

    /**
     * JWT Authentication Filter for API Gateway.
     * <p>
     * Validates JWT from HttpOnly cookie, checks Redis blacklist,
     * and populates Spring Security context.
     */
    public JwtAuthWebFilter(CookieUtil cookieUtil,
                            TokenBlacklistService tokenBlacklistService,
                            JwtService jwtService, SecurityResponseWriter responseWriter) {

        this.cookieUtil = cookieUtil;
        this.tokenBlacklistService = tokenBlacklistService;
        this.jwtService = jwtService;
        this.responseWriter = responseWriter;
    }

    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange,
                             @NonNull WebFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // 1. Skip logic for paths that MUST be public at the filter level (like Actuator)
        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }


        String token = cookieUtil.extractToken(exchange);
        // 2. If no token is found, do NOT return unauthorized.
        // Just move to the next filter. Spring Security will handle the 403/401
        // based on your pathMatchers later.
        if (token == null || token.isBlank()) {
            return chain.filter(exchange);
        }


        // 3. If a token exists, validate it
        return tokenBlacklistService.isBlacklisted(token)
                .flatMap(isBlacklisted -> {

                    if (Boolean.TRUE.equals(isBlacklisted)) {
                        // Validating a BAD token still fails
                        return unauthorized(exchange);
                    }

                    return Mono.fromCallable(() -> {

                                var claims = jwtService.parseClaims(token);
                                jwtService.validateClaims(claims);

                                Long userId = Long.parseLong(claims.getSubject());
                                String role = claims.get("role", String.class);

                                var principal = new com.itc.funkart.gateway.dto.UserDto(
                                        userId,
                                        null,
                                        null,
                                        role
                                );

                                return new UsernamePasswordAuthenticationToken(
                                        principal,
                                        null,
                                        List.of(new SimpleGrantedAuthority(role))
                                );
                            })
                            .flatMap(auth ->
                                    chain.filter(exchange)
                                            .contextWrite(
                                                    ReactiveSecurityContextHolder.withAuthentication(auth)
                                            )
                            )
                            .onErrorResume(JwtAuthenticationException.class, e ->
                                    responseWriter.writeUnauthorized(exchange)
                            );
                });
    }

    /**
     * Returns 401 Unauthorized and terminates request.
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}