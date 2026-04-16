package com.itc.funkart.gateway.controller;

import com.itc.funkart.gateway.config.NoApiPrefix;
import com.itc.funkart.gateway.config.props.FrontendProperties;
import com.itc.funkart.gateway.config.props.GitHubProperties;
import com.itc.funkart.gateway.response.ApiResponse;
import com.itc.funkart.gateway.security.CookieUtil;
import com.itc.funkart.gateway.service.GithubOAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * <h2>GitHub OAuth2 Entry Point</h2>
 * Handles the redirect-heavy handshake with GitHub.
 * Annotated with {@code @NoApiPrefix} to keep URLs clean (e.g., /oauth/github/login).
 */
@NoApiPrefix
@RestController
@RequestMapping("/oauth/github")
public class GithubOAuthController {

    private final CookieUtil cookieUtil;
    private final GithubOAuthService githubOAuthService;
    private final GitHubProperties gitHubProperties;
    private final FrontendProperties frontendProperties;

    public GithubOAuthController(CookieUtil cookieUtil,
                                 GithubOAuthService githubOAuthService,
                                 FrontendProperties frontendProperties,
                                 GitHubProperties gitHubProperties) {
        this.cookieUtil = cookieUtil;
        this.githubOAuthService = githubOAuthService;
        this.frontendProperties = frontendProperties;
        this.gitHubProperties = gitHubProperties;
    }

    /** Redirects the user to GitHub to begin the OAuth flow. */
    @GetMapping("/login")
    public Mono<ResponseEntity<Void>> login() {
        String githubAuthUrl = UriComponentsBuilder.fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", gitHubProperties.clientId())
                .queryParam("redirect_uri", gitHubProperties.redirectUri())
                .queryParam("scope", "user:email")
                .toUriString();

        return Mono.just(ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                .location(URI.create(githubAuthUrl))
                .build());
    }

    /** * Receives the 'code' from GitHub and converts it to a cookie-based session.
     * Finally redirects back to the Frontend (e.g., localhost:3000).
     */
    @GetMapping("/callback")
    public Mono<ResponseEntity<Void>> callback(@RequestParam String code, ServerWebExchange exchange) {
        return githubOAuthService.processCode(code)
                .doOnNext(jwt -> cookieUtil.addTokenCookie(exchange, jwt, null))
                .then(Mono.just(ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                        .location(URI.create(frontendProperties.url()))
                        .build()));
    }

    /** Standardized Logout. Clears the cookie and returns a unified response. */
    @GetMapping("/logout")
    public Mono<ResponseEntity<ApiResponse<Void>>> logout(ServerWebExchange exchange) {
        cookieUtil.clearTokenCookie(exchange);
        return Mono.just(ResponseEntity.ok(new ApiResponse<>(null, "Logged out successfully")));
    }
}