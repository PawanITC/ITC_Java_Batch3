package com.itc.funkart.gateway.service;

import com.itc.funkart.gateway.dto.request.CodeRequest;
import com.itc.funkart.gateway.dto.response.OAuthResponse;
import com.itc.funkart.gateway.exception.OAuthException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Service for GitHub OAuth flow.
 * Routes to User-Service for actual OAuth processing.
 * Receives JWT token back from User-Service.
 */
@Service
public class GithubOAuthService {

    private final WebClient webClient;

    public GithubOAuthService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Process GitHub OAuth code by delegating to User-Service
     *
     * @param code GitHub OAuth authorization code
     * @return Mono<String> JWT token
     */
    public Mono<String> processCode(String code) {
        return webClient.post()
                .uri("/api/v1/users/oauth/github")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CodeRequest(code))
                .retrieve()
                .bodyToMono(OAuthResponse.class)
                .switchIfEmpty(Mono.error(() -> new OAuthException("User service returned null")))
                .map(OAuthResponse::token)
                .onErrorMap(WebClientResponseException.class,
                        ex -> new OAuthException("User service returned error: " + ex.getStatusCode(), ex))
                .onErrorMap(ex -> !(ex instanceof OAuthException),
                        ex -> new OAuthException("Failed to process GitHub OAuth", ex));
    }
}