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

/**
 * REST Controller responsible for user authentication and account management.
 * <p>
 * This controller acts as a proxy for the downstream User Service. It captures
 * authentication requests, forwards them to the microservice, and converts the
 * resulting JWT tokens into secure HttpOnly cookies for the frontend.
 * </p>
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final WebClient webClient;
    private final CookieUtil cookieUtil;

    /**
     * Constructs a new UserController with required dependencies.
     *
     * @param webClient  The reactive web client used to communicate with downstream services.
     * @param cookieUtil Utility for managing secure JWT cookies on the ServerWebExchange.
     */
    public UserController(WebClient webClient, CookieUtil cookieUtil) {
        this.webClient = webClient;
        this.cookieUtil = cookieUtil;
    }

    /**
     * Authenticates a user and sets an identity cookie.
     * <p>
     * Forwards the {@link LoginRequest} to {@code /api/v1/users/login}. Upon success,
     * extracts the JWT from the response and attaches it to the client's cookies.
     * </p>
     *
     * @param request  The user's credentials (email and password).
     * @param exchange The current server exchange used to set the response cookie.
     * @return A {@link Mono} emitting a 200 OK ResponseEntity on success, or an error signal.
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponse<SuccessfulLoginResponse>>> login(
            @RequestBody LoginRequest request,
            ServerWebExchange exchange) {

        return webClient.post()
                .uri("/api/v1/users/login")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<SuccessfulLoginResponse>>() {
                })
                .doOnNext(response -> {
                    String jwt = response.getData().token();
                    cookieUtil.addTokenCookie(exchange, jwt, null);
                })
                .map(ResponseEntity::ok);
    }

    /**
     * Registers a new user and automatically signs them in.
     * <p>
     * Forwards the {@link SignupRequest} to {@code /api/v1/users/signup}. Upon successful
     * registration, the method mirrors the login flow by setting a secure JWT cookie.
     * </p>
     *
     * @param request  The registration details including name, email, and password.
     * @param exchange The current server exchange used to set the response cookie.
     * @return A {@link Mono} emitting a 200 OK ResponseEntity on success, or an error signal.
     */
    @PostMapping("/signup")
    public Mono
            <ResponseEntity<ApiResponse<SuccessfulLoginResponse>>> signup(
            @RequestBody SignupRequest request,
            ServerWebExchange exchange) {

        return webClient.post()
                .uri("/api/v1/users/signup")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<SuccessfulLoginResponse>>() {
                })
                .doOnNext(response -> {
                    String jwt = response.getData().token();
                    cookieUtil.addTokenCookie(exchange, jwt, null);
                })
                .map(ResponseEntity::ok);
    }
}