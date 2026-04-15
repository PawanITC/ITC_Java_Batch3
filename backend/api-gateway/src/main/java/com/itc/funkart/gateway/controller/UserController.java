package com.itc.funkart.gateway.controller;

import com.itc.funkart.gateway.config.AppConfig;
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
    private final String userBaseUri;

    public UserController(WebClient webClient, CookieUtil cookieUtil, AppConfig appConfig) {
        this.webClient = webClient;
        this.cookieUtil = cookieUtil;
        // Dynamically build the path based on version config
        this.userBaseUri = appConfig.api().version() + "/users";
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponse<SuccessfulLoginResponse>>> login(
            @RequestBody LoginRequest request, ServerWebExchange exchange) {

        return webClient.post()
                .uri(userBaseUri + "/login")
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
                .uri(userBaseUri + "/signup")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<SuccessfulLoginResponse>>() {})
                .doOnNext(response -> cookieUtil.addTokenCookie(exchange, response.getData().token(), null))
                .map(ResponseEntity::ok);
    }
}