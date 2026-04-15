package com.itc.funkart.gateway.service;

import com.itc.funkart.gateway.config.AppConfig;
import com.itc.funkart.gateway.dto.request.CodeRequest;
import com.itc.funkart.gateway.dto.response.OAuthResponse;
import com.itc.funkart.gateway.exception.OAuthException;
import com.itc.funkart.gateway.response.ApiResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Service for GitHub OAuth flow proxying.
 * Bridges the Gateway and the User-Service to exchange GitHub codes for JWTs.
 */
@Service
public class GithubOAuthService {

    private final WebClient webClient;
    private final String oauthUri;

    public GithubOAuthService(WebClient webClient, AppConfig appConfig) {
        this.webClient = webClient;
        // Dynamically build URI using the config version: /api/v1/users/oauth/github
        this.oauthUri = appConfig.api().version() + "/users/oauth/github";
    }

    /**
     * Delegates OAuth processing to the User-Service.
     * * @param code GitHub authorization code.
     * @return Mono containing the verified JWT string.
     */
    public Mono<String> processCode(String code) {
        return webClient.post()
                .uri(oauthUri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CodeRequest(code))
                .retrieve()
                // Use ParameterizedTypeReference to unwrap the ApiResponse<OAuthResponse>
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<OAuthResponse>>() {})
                .flatMap(response -> {
                    if (response.getData() != null && response.getData().token() != null) {
                        return Mono.just(response.getData().token());
                    }
                    return Mono.error(new OAuthException("OAuth response was successful but contained no token"));
                })
                .onErrorMap(WebClientResponseException.class, ex ->
                        new OAuthException("User-Service authentication failed: " + ex.getResponseBodyAsString(), ex))
                .onErrorMap(ex -> !(ex instanceof OAuthException),
                        ex -> new OAuthException("Communication failure with User-Service", ex));
    }
}