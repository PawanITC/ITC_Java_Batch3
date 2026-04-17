package com.itc.funkart.gateway.controller;

import com.itc.funkart.gateway.config.AppConfig;
import com.itc.funkart.gateway.config.NoApiPrefix;
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
 * <h2>GitHub OAuth2 Controller</h2>
 * <p>
 * Manages the multistep OAuth2 handshake with GitHub. This controller is responsible for:
 * <ul>
 * <li>Initiating the redirect to GitHub's authorization server.</li>
 * <li>Handling the callback and exchanging the auth code for a system JWT.</li>
 * <li>Abstracting the JWT into a secure HttpOnly cookie.</li>
 * </ul>
 * </p>
 */
@NoApiPrefix
@RestController
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
     * Redirects the end-user to the GitHub OAuth authorization page.
     *
     * @return A 307 Temporary Redirect to GitHub.
     */
    @GetMapping("/login")
    public Mono<ResponseEntity<Void>> login() {
        String githubAuthUrl = UriComponentsBuilder.fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", appConfig.github().clientId())
                .queryParam("redirect_uri", appConfig.github().redirectUri())
                .queryParam("scope", "user:email")
                .toUriString();

        return Mono.just(ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                .location(URI.create(githubAuthUrl))
                .build());
    }

    /**
     * Handles the callback from GitHub after user authorization.
     * <p>
     * Exchanges the code for a JWT via {@link GithubOAuthService}, attaches it as a cookie,
     * and redirects the user back to the application frontend.
     * </p>
     *
     * @param code     The temporary authorization code provided by GitHub.
     * @param exchange The current server exchange to modify response cookies.
     * @return A redirect to the configured frontend URL.
     */
    @GetMapping("/callback")
    public Mono<ResponseEntity<Void>> callback(@RequestParam String code, ServerWebExchange exchange) {
        return githubOAuthService.processCode(code)
                .flatMap(jwt -> cookieUtil.addTokenCookie(exchange, jwt))
                .then(Mono.just(ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                        .location(URI.create(appConfig.frontendUrl()))
                        .build()));
    }

    /**
     * Invalidates the user session by clearing the security cookie.
     *
     * @param exchange The current server exchange.
     * @return A success message wrapped in an {@link ApiResponse}.
     */
    @GetMapping("/logout")
    public Mono<ResponseEntity<ApiResponse<Void>>> logout(ServerWebExchange exchange) {
        return cookieUtil.clearTokenCookie(exchange)
                .then(Mono.just(ResponseEntity.ok(new ApiResponse<>(null, "Logged out successfully"))));
    }
}