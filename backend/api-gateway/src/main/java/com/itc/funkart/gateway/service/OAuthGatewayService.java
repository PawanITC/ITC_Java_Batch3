package com.itc.funkart.gateway.service;

import com.itc.funkart.gateway.config.AppConfig;
import com.itc.funkart.gateway.dto.UserDto;
import com.itc.funkart.gateway.dto.request.CodeRequest;
import com.itc.funkart.gateway.dto.response.OAuthResponse;
import com.itc.funkart.gateway.dto.response.SuccessfulLoginResponse;
import com.itc.funkart.gateway.exception.JwtAuthenticationException;
import com.itc.funkart.gateway.response.ApiResponse;
import com.itc.funkart.gateway.security.CookieUtil;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Service
public class OAuthGatewayService {

    private final WebClient webClient;
    private final AppConfig appConfig;
    private final CookieUtil cookieUtil;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;

    public OAuthGatewayService(
            WebClient webClient,
            AppConfig appConfig,
            CookieUtil cookieUtil,
            JwtService jwtService,
            TokenBlacklistService tokenBlacklistService, RefreshTokenService refreshTokenService
    ) {
        this.webClient = webClient;
        this.appConfig = appConfig;
        this.cookieUtil = cookieUtil;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.refreshTokenService = refreshTokenService;
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
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<OAuthResponse>>() {
                })
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

        String token = cookieUtil.extractToken(exchange);

        if (token == null || token.isBlank()) {
            return cookieUtil.clearTokenCookie(exchange);
        }

        return Mono.fromCallable(() -> jwtService.getExpiration(token))
                .flatMap(expiry ->
                        tokenBlacklistService.blacklistWithExpiry(token, expiry)
                )
                .then(cookieUtil.clearTokenCookie(exchange));
    }


    public Mono<ApiResponse<SuccessfulLoginResponse>> refresh(
            String refreshToken,
            ServerWebExchange exchange
    ) {

        String deviceId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Device-Id");

        if (deviceId == null || deviceId.isBlank()) {
            return Mono.error(new JwtAuthenticationException("Missing device id"));
        }

        return refreshTokenService.validateAndConsume(refreshToken, deviceId)
                .flatMap(userId -> {

                    Mono<UserDto> userMono = webClient
                            .get()
                            .uri("/users/{id}", userId)
                            .retrieve()
                            .bodyToMono(UserDto.class);

                    return userMono.flatMap(user -> {


                        String role = user.role();

                        String newAccessToken = jwtService.generateAccessToken(userId, role);
                        String newRefreshToken = UUID.randomUUID().toString();

                        Duration refreshTtl = Duration.ofDays(7);

                        Mono<Void> storeRefresh = refreshTokenService.store(
                                newRefreshToken,
                                userId,
                                deviceId,
                                refreshTtl
                        );

                        Mono<Void> setCookies =
                                cookieUtil.addTokenCookie(exchange, newAccessToken)
                                        .then(cookieUtil.addRefreshCookie(exchange, newRefreshToken));

                        return storeRefresh
                                .then(setCookies)
                                .thenReturn(new ApiResponse<>(
                                        new SuccessfulLoginResponse(user, newAccessToken),
                                        "Token refreshed successfully"
                                ));
                    });
                });
    }

    public String frontendRedirect() {
        return appConfig.frontendUrl();
    }
}
