package com.itc.funkart.gateway.service;

import com.itc.funkart.gateway.config.AppConfig;
import com.itc.funkart.gateway.dto.request.CodeRequest;
import com.itc.funkart.gateway.dto.response.OAuthResponse;
import com.itc.funkart.gateway.response.ApiResponse;
import com.itc.funkart.gateway.security.CookieUtil;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Service
public class OAuthGatewayService {

    private final WebClient webClient;
    private final AppConfig appConfig;
    private final CookieUtil cookieUtil;

    public OAuthGatewayService(WebClient webClient,
                               AppConfig appConfig,
                               CookieUtil cookieUtil) {
        this.webClient = webClient;
        this.appConfig = appConfig;
        this.cookieUtil = cookieUtil;
    }

    public String buildGithubRedirectUrl() {
        return UriComponentsBuilder.fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", appConfig.github().clientId())
                .queryParam("redirect_uri", appConfig.github().redirectUri())
                .queryParam("scope", "user:email")
                .toUriString();
    }

    public Mono<Void> handleCallback(String code, ServerWebExchange exchange) {
        return webClient.post()
                .uri("/api/v1/users/oauth/github")
                .bodyValue(new CodeRequest(code))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<OAuthResponse>>() {})
                .flatMap(response ->
                        cookieUtil.addTokenCookie(exchange, response.getData().token())
                );
    }

    /**
     * Clears the authentication session by removing the JWT cookie.
     *
     * <p>
     * This does NOT communicate with the User-Service, it is a gateway-level operation only.
     * It only handles browser session cleanup at the Gateway layer.
     * </p>
     */
    public Mono<Void> logout(ServerWebExchange exchange) {
        return cookieUtil.clearTokenCookie(exchange);
    }

    public String frontendRedirect() {
        return appConfig.frontendUrl();
    }
}
