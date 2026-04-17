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
 * <p>Handles the lifecycle of the JWT Session Cookie using Reactive patterns.</p>
 */
@Component
public class CookieUtil {

    private final AppConfig appConfig;

    public CookieUtil(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * Issues a secure JWT cookie wrapped in a Mono.
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
     * Extracts token from the request cookies.
     */
    public String extractToken(ServerWebExchange exchange) {
        HttpCookie cookie = exchange.getRequest()
                .getCookies()
                .getFirst(appConfig.jwt().cookieName());

        return (cookie != null) ? cookie.getValue() : null;
    }

    /**
     * Clears the session cookie by setting maxAge to zero.
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
}