package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.AppConfig;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * <h2>Cookie Management Utility</h2>
 * <p>
 * Centralized utility for managing JWT session cookies in a reactive gateway.
 * Responsible only for transport-layer concerns (NOT authentication logic).
 * </p>
 */
@Component
public class CookieUtil {

    private final AppConfig appConfig;

    public CookieUtil(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * Issues a secure HttpOnly JWT cookie.
     */
    public Mono<Void> addTokenCookie(ServerWebExchange exchange, String token) {
        return Mono.fromRunnable(() -> {

            ResponseCookie cookie = ResponseCookie
                    .from(appConfig.jwt().cookieName(), token)
                    .httpOnly(true)
                    .secure(appConfig.jwt().secureCookie())
                    .path("/")
                    .maxAge(Duration.ofSeconds(appConfig.jwt().cookieMaxAgeSeconds()))
                    .sameSite(appConfig.jwt().secureCookie() ? "None" : "Lax")
                    .build();

            exchange.getResponse().addCookie(cookie);
        });
    }

    /**
     * Extracts JWT token from request (cookie-based primary source).
     */
    public String extractToken(ServerWebExchange exchange) {

        HttpCookie cookie = exchange.getRequest()
                .getCookies()
                .getFirst(appConfig.jwt().cookieName());

        if (cookie != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }

        // Optional future support (safe fallback for mobile / external clients)
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    /**
     * Clears JWT session cookie (logout operation).
     */
    public Mono<Void> clearTokenCookie(ServerWebExchange exchange) {
        return Mono.fromRunnable(() -> {

            ResponseCookie cookie = ResponseCookie
                    .from(appConfig.jwt().cookieName(), "")
                    .httpOnly(true)
                    .secure(appConfig.jwt().secureCookie())
                    .path("/")
                    .maxAge(Duration.ZERO)
                    .sameSite(appConfig.jwt().secureCookie() ? "None" : "Lax")
                    .build();

            exchange.getResponse().addCookie(cookie);
        });
    }


    public Mono<Void> addRefreshCookie(ServerWebExchange exchange, String token) {
        return Mono.fromRunnable(() -> {

            ResponseCookie cookie = ResponseCookie
                    .from("refresh_token", token)
                    .httpOnly(true)
                    .secure(appConfig.jwt().secureCookie())
                    .path("/")
                    .maxAge(Duration.ofDays(7))
                    .sameSite("Strict")
                    .build();

            exchange.getResponse().addCookie(cookie);
        });
    }
}