package com.itc.funkart.gateway.controller;

import com.itc.funkart.gateway.config.GithubOAuthConfig;
import com.itc.funkart.gateway.security.CookieUtil;
import com.itc.funkart.gateway.service.GithubOAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/oauth/github")
public class GithubOAuthController {

    private final CookieUtil cookieUtil;
    private final GithubOAuthConfig githubOAuthConfig;
    private final GithubOAuthService githubOAuthService;

    @Value("${frontend.url}")
    private String frontendUrl;

    public GithubOAuthController(CookieUtil cookieUtil,
                                 GithubOAuthConfig githubOAuthConfig,
                                 GithubOAuthService githubOAuthService) {
        this.cookieUtil = cookieUtil;
        this.githubOAuthConfig = githubOAuthConfig;
        this.githubOAuthService = githubOAuthService;
    }

    /**
     * Initiates GitHub OAuth flow
     */
    @GetMapping("/login")
    public Mono<ResponseEntity<Void>> login() {
        String githubAuthUrl = UriComponentsBuilder
                .fromHttpUrl("https://github.com/login/oauth/authorize")
                .queryParam("client_id", githubOAuthConfig.getClientId())
                .queryParam("redirect_uri", githubOAuthConfig.getRedirectUri())
                .queryParam("scope", "user:email")
                .toUriString();

        return Mono.just(ResponseEntity
                .status(HttpStatus.TEMPORARY_REDIRECT)
                .location(URI.create(githubAuthUrl))
                .build());
    }

    /**
     * GitHub OAuth Callback Handler
     */
    @GetMapping("/callback")
    public Mono<ResponseEntity<Void>> callback(@RequestParam String code, ServerWebExchange exchange) {
        return githubOAuthService.processCode(code)
                .doOnNext(jwt -> {
                    // Set JWT cookie BEFORE building response
                    cookieUtil.addTokenCookie(exchange, jwt, null);
                })
                .map(jwt -> ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                        .location(URI.create(frontendUrl))
                        .build());
    }

    /**
     * Logout Endpoint
     */
    @GetMapping("/logout")
    public Mono<ResponseEntity<Map<String, String>>> logout(ServerWebExchange exchange) {
        cookieUtil.clearTokenCookie(exchange);

        return Mono.just(ResponseEntity.ok(
                Map.of("message", "Logged out successfully")
        ));
    }
}