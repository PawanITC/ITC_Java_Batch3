package com.itc.funkart.gateway.controller;

import com.itc.funkart.gateway.dto.request.LoginRequest;
import com.itc.funkart.gateway.dto.request.SignupRequest;
import com.itc.funkart.gateway.dto.response.SuccessfulLoginResponse;
import com.itc.funkart.gateway.response.ApiResponse;
import com.itc.funkart.gateway.security.CookieUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <h2>User Authentication Gateway Proxy</h2>
 * <p>
 * Intercepts standard authentication requests (Login/Signup) and proxies them to the
 * downstream User Microservice. Its primary security responsibility is to extract
 * the plaintext JWT from the response and encapsulate it in a secure HttpOnly cookie.
 * </p>
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final WebClient webClient;
    private final CookieUtil cookieUtil;

    /**
     * @param webClient Injected {@code userWebClient} configured for the User-Service base URL.
     * @param cookieUtil Utility for reactive cookie management.
     */
    public UserController(@Qualifier("userWebClient") WebClient webClient,
                          CookieUtil cookieUtil) {
        this.webClient = webClient;
        this.cookieUtil = cookieUtil;
    }

    /**
     * Proxies login credentials and establishes a secure session cookie.
     *
     * @param request  The login credentials (email/password).
     * @param exchange The current server exchange.
     * @return The User profile data with the token abstracted into a cookie.
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponse<SuccessfulLoginResponse>>> login(
            @RequestBody LoginRequest request, ServerWebExchange exchange) {

        return webClient.post()
                .uri("/users/login")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<SuccessfulLoginResponse>>() {})
                .flatMap(response ->
                        cookieUtil.addTokenCookie(exchange, response.getData().token())
                                .thenReturn(ResponseEntity.ok(response))
                );
    }

    /**
     * Proxies registration data and establishes a secure session cookie upon success.
     *
     * @param request  The registration details.
     * @param exchange The current server exchange.
     * @return The created User profile data.
     */
    @PostMapping("/signup")
    public Mono<ResponseEntity<ApiResponse<SuccessfulLoginResponse>>> signup(
            @RequestBody SignupRequest request, ServerWebExchange exchange) {

        return webClient.post()
                .uri("/users/signup")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<SuccessfulLoginResponse>>() {})
                .flatMap(response ->
                        cookieUtil.addTokenCookie(exchange, response.getData().token())
                                .thenReturn(ResponseEntity.ok(response))
                );
    }
}