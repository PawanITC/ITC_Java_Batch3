package com.itc.funkart.gateway.service;

import com.itc.funkart.gateway.dto.request.LoginRequest;
import com.itc.funkart.gateway.dto.request.SignupRequest;
import com.itc.funkart.gateway.dto.response.SuccessfulLoginResponse;
import com.itc.funkart.gateway.response.ApiResponse;
import com.itc.funkart.gateway.security.CookieUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Service
public class UserGatewayService {

    private final WebClient webClient;
    private final CookieUtil cookieUtil;

    public UserGatewayService(@Qualifier("userWebClient") WebClient webClient,
                              CookieUtil cookieUtil) {
        this.webClient = webClient;
        this.cookieUtil = cookieUtil;
    }

    public Mono<ApiResponse<SuccessfulLoginResponse>> login(LoginRequest request, ServerWebExchange exchange) {
        return webClient.post()
                .uri("/users/login")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<SuccessfulLoginResponse>>() {})
                .flatMap(response ->
                        cookieUtil.addTokenCookie(exchange, response.getData().token())
                                .thenReturn(response)
                );
    }

    public Mono<ApiResponse<SuccessfulLoginResponse>> signup(SignupRequest request, ServerWebExchange exchange) {
        return webClient.post()
                .uri("/users/signup")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<SuccessfulLoginResponse>>() {})
                .flatMap(response ->
                        cookieUtil.addTokenCookie(exchange, response.getData().token())
                                .thenReturn(response)
                );
    }
}
