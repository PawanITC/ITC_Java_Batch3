package com.itc.funkart.gateway.controller;

import com.itc.funkart.gateway.config.AppConfig;
import com.itc.funkart.gateway.config.NoApiPrefix;
import com.itc.funkart.gateway.security.CookieUtil;
import com.itc.funkart.gateway.service.GithubOAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

/**
 * Controller handling GitHub OAuth2 authentication flow.
 * Acts as the entry point for frontend login and the callback handler for GitHub.
 */
@RestController
@NoApiPrefix
@RequestMapping("/oauth/github")
public class GithubOAuthController {

    private final CookieUtil cookieUtil;
    private final GithubOAuthService githubOAuthService;
    private final AppConfig appConfig;

    public GithubOAuthController(CookieUtil cookieUtil,
                                 GithubOAuthService githubOAuthService,
                                 AppConfig appConfig) {
        this.cookieUtil = cookieUtil;
        this.githubOAuthService = githubOAuthService;
        this.appConfig = appConfig;
    }

    /**
     * Initiates GitHub OAuth flow by redirecting the user to GitHub's authorization page.
     */
    @GetMapping("/login")
    public Mono<ResponseEntity<Void>> login() {
        String githubAuthUrl = UriComponentsBuilder
                .fromHttpUrl("https://github.com/login/oauth/authorize")
                .queryParam("client_id", appConfig.github().clientId())
                .queryParam("redirect_uri", appConfig.github().redirectUri())
                .queryParam("scope", "user:email")
                .toUriString();

        return Mono.just(ResponseEntity
                .status(HttpStatus.TEMPORARY_REDIRECT)
                .location(URI.create(githubAuthUrl))
                .build());
    }

    /**
     * GitHub OAuth Callback Handler.
     * Receives the 'code' from GitHub, exchanges it for a JWT via the GithubOAuthService,
     * sets the JWT cookie, and redirects to the frontend.
     */
    @GetMapping("/callback")
    public Mono<ResponseEntity<Void>> callback(@RequestParam String code, ServerWebExchange exchange) {
        return githubOAuthService.processCode(code)
                .doOnNext(jwt -> cookieUtil.addTokenCookie(exchange, jwt, null))
                .map(jwt -> ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                        .location(URI.create(appConfig.frontendUrl()))
                        .build());
    }

    /**
     * Logout Endpoint.
     * Clears the JWT cookie and returns a success message.
     */
    @GetMapping("/logout")
    public Mono<ResponseEntity<Map<String, String>>> logout(ServerWebExchange exchange) {
        cookieUtil.clearTokenCookie(exchange);

        return Mono.just(ResponseEntity.ok(
                Map.of("message", "Logged out successfully")
        ));
    }
}