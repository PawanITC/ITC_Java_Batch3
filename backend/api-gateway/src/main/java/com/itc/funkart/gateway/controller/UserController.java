package com.itc.funkart.gateway.controller;

import com.itc.funkart.gateway.dto.request.LoginRequest;
import com.itc.funkart.gateway.dto.request.SignupRequest;
import com.itc.funkart.gateway.dto.response.SuccessfulLoginResponse;
import com.itc.funkart.gateway.response.ApiResponse;
import com.itc.funkart.gateway.security.CookieUtil;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <h2>Authentication Proxy</h2>
 * Intercepts Login/Signup requests to set secure HttpOnly cookies.
 * This prevents the Frontend from ever needing to store raw JWTs.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final WebClient webClient;
    private final CookieUtil cookieUtil;

    public UserController(WebClient webClient,
                          CookieUtil cookieUtil) {
        this.webClient = webClient;
        this.cookieUtil = cookieUtil;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponse<SuccessfulLoginResponse>>> login(
            @RequestBody LoginRequest request, ServerWebExchange exchange) {

        return webClient.post()
                .uri("/users/login")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<SuccessfulLoginResponse>>() {})
                .doOnNext(response -> cookieUtil.addTokenCookie(exchange, response.getData().token(), null))
                .map(ResponseEntity::ok);
    }

    @PostMapping("/signup")
    public Mono<ResponseEntity<ApiResponse<SuccessfulLoginResponse>>> signup(
            @RequestBody SignupRequest request, ServerWebExchange exchange) {

        return webClient.post()
                .uri("/users/signup")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<SuccessfulLoginResponse>>() {})
                .doOnNext(response -> cookieUtil.addTokenCookie(exchange, response.getData().token(), null))
                .map(ResponseEntity::ok);
    }
}