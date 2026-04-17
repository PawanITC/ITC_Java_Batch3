package com.itc.funkart.gateway.service;

import com.itc.funkart.gateway.config.props.ApiProperties;
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

    public GithubOAuthService(WebClient webClient, ApiProperties apiProperties) {
        this.webClient = webClient;

        // "cache" computed once at startup (safe + immutable)
        this.oauthUri = apiProperties.version() + "/users/oauth/github";
    }

    /**
     * Delegates OAuth processing to the User-Service.
     *
     * @param code GitHub authorization code.
     * @return Mono containing the verified JWT string.
     */
    public Mono<String> processCode(String code) {
        return webClient.post()
                .uri(oauthUri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CodeRequest(code))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<OAuthResponse>>() {})
                .onErrorMap(WebClientResponseException.class, ex ->
                        new OAuthException(
                                "User-Service authentication failed: " + ex.getResponseBodyAsString(),
                                ex))
                .flatMap(response -> {
                    var data = response.getData();

                    if (data == null || data.token() == null) {
                        return Mono.error(
                                new OAuthException("OAuth response was successful but contained no token")
                        );
                    }

                    return Mono.just(data.token());
                })
                .onErrorMap(ex -> !(ex instanceof OAuthException),
                        ex -> new OAuthException("Communication failure with User-Service", ex));
    }
}