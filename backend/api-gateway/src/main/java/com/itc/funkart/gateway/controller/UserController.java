package com.itc.funkart.gateway.controller;

import com.itc.funkart.gateway.dto.request.LoginRequest;
import com.itc.funkart.gateway.dto.request.SignupRequest;
import com.itc.funkart.gateway.dto.response.SuccessfulLoginResponse;
import com.itc.funkart.gateway.response.ApiResponse;
import com.itc.funkart.gateway.security.CookieUtil;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final WebClient webClient;
    private final CookieUtil cookieUtil;

    public UserController(WebClient webClient, CookieUtil cookieUtil) {
        this.webClient = webClient;
        this.cookieUtil = cookieUtil;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<Void>> login(
            @RequestBody LoginRequest request,
            ServerWebExchange exchange) {

        return webClient.post()
                .uri("/api/v1/users/login")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<SuccessfulLoginResponse>>() {})
                .doOnNext(response -> {
                    String jwt = response.getData().token();
                    cookieUtil.addTokenCookie(exchange, jwt, null);
                })
                .map(response -> ResponseEntity.ok().build());
    }

    @PostMapping("/signup")
    public Mono<ResponseEntity<Void>> signup(
            @RequestBody SignupRequest request,
            ServerWebExchange exchange) {

        return webClient.post()
                .uri("/api/v1/users/signup")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<SuccessfulLoginResponse>>() {})
                .doOnNext(response -> {
                    String jwt = response.getData().token();
                    cookieUtil.addTokenCookie(exchange, jwt, null);
                })
                .map(response -> ResponseEntity.ok().build());
    }
}