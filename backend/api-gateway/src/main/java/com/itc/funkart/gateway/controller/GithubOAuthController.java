package com.itc.funkart.gateway.controller;

import com.itc.funkart.gateway.config.NoApiPrefix;
import com.itc.funkart.gateway.service.OAuthGatewayService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * <h2>GitHub OAuth Gateway Controller</h2>
 *
 * <p>
 * Handles the OAuth2 browser redirect flow for GitHub authentication.
 * This controller acts as a thin orchestration layer and delegates all
 * business logic to {@link OAuthGatewayService}.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Redirect user to GitHub OAuth authorization endpoint</li>
 *   <li>Handle OAuth callback from GitHub</li>
 *   <li>Delegate token exchange and session creation</li>
 *   <li>Redirect user back to frontend after authentication</li>
 * </ul>
 */
@NoApiPrefix
@RestController
@RequestMapping("/oauth/github")
public class GithubOAuthController {

    private final OAuthGatewayService oAuthGatewayService;

    public GithubOAuthController(OAuthGatewayService oAuthGatewayService) {
        this.oAuthGatewayService = oAuthGatewayService;
    }

    /**
     * Initiates GitHub OAuth login by redirecting the user
     * to the GitHub authorization endpoint.
     *
     * @return HTTP 307 redirect to GitHub login page
     */
    @GetMapping("/login")
    public Mono<ResponseEntity<Void>> login() {
        String url = oAuthGatewayService.buildGithubRedirectUrl();

        return Mono.just(
                ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                        .location(URI.create(url))
                        .build()
        );
    }

    /**
     * Handles OAuth callback from GitHub.
     *
     * <p>
     * Exchanges authorization code for JWT via User-Service,
     * stores session token in secure cookie, and redirects
     * user to frontend application.
     * </p>
     *
     * @param code     GitHub authorization code
     * @param exchange reactive server exchange context
     * @return redirect response to frontend
     */
    @GetMapping("/callback")
    public Mono<ResponseEntity<Void>> callback(
            @RequestParam String code,
            ServerWebExchange exchange) {

        return oAuthGatewayService.handleCallback(code, exchange)
                .then(Mono.just(
                        ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                                .location(URI.create(oAuthGatewayService.frontendRedirect()))
                                .build()
                ));
    }

    /**
     * <h2>Session Logout Endpoint</h2>
     *
     * <p>
     * Terminates the client-side authentication session by clearing the
     * HttpOnly JWT cookie stored in the browser.
     * </p>
     *
     * <h3>Important</h3>
     * <ul>
     *   <li>This does NOT invalidate server-side authentication state</li>
     *   <li>It only removes the browser session token (gateway responsibility)</li>
     *   <li>Actual token/session revocation (if applicable) is handled by User-Service</li>
     * </ul>
     *
     * @param exchange reactive HTTP exchange used to manipulate cookies
     * @return confirmation response indicating session termination
     */
    @GetMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(ServerWebExchange exchange) {
        return oAuthGatewayService.logout(exchange)
                .thenReturn(ResponseEntity.noContent().build());
    }
}