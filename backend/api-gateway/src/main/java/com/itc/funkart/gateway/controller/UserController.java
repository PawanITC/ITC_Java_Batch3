package com.itc.funkart.gateway.controller;

import com.itc.funkart.common.dto.auth.request.login.LoginRequest;
import com.itc.funkart.common.dto.auth.request.signup.SignupRequest;
import com.itc.funkart.common.dto.auth.response.login.SuccessfulLoginResponse;
import com.itc.funkart.common.dto.response.ApiResponse;
import com.itc.funkart.gateway.security.CookieUtil;
import com.itc.funkart.gateway.service.UserGatewayService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <h2>User Authentication Gateway Controller</h2>
 *
 * <p>
 * Entry point for authentication-related HTTP requests (login and signup).
 * This controller is intentionally thin and delegates all business logic
 * and downstream communication to {@link UserGatewayService}.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Accept incoming HTTP requests from the client</li>
 *   <li>Delegate authentication flows to the Gateway service layer</li>
 *   <li>Return standardized API responses</li>
 * </ul>
 *
 * <h3>Non-Responsibilities</h3>
 * <ul>
 *   <li>Does NOT call downstream services directly</li>
 *   <li>Does NOT manage cookies or tokens</li>
 *   <li>Does NOT contain business or orchestration logic</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserGatewayService userGatewayService;
    private final CookieUtil cookieUtil;

    /**
     * @param userGatewayService Service responsible for handling authentication
     *                           orchestration and communication with the User-Service.
     * @param cookieUtil         Utility for clearing the JWT session cookie on logout.
     */
    public UserController(UserGatewayService userGatewayService, CookieUtil cookieUtil) {
        this.userGatewayService = userGatewayService;
        this.cookieUtil = cookieUtil;
    }

    /**
     * Handles user login requests by delegating authentication to the Gateway service layer.
     *
     * <p>
     * The service layer performs the downstream call to the User-Service,
     * processes the response, and manages session establishment (e.g. cookie handling).
     * </p>
     *
     * @param request  The login credentials (email/password)
     * @param exchange The current server exchange context
     * @return A successful authentication response containing user data
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponse<SuccessfulLoginResponse>>> login(
            @RequestBody LoginRequest request,
            ServerWebExchange exchange) {

        return userGatewayService.login(request, exchange)
                .map(ResponseEntity::ok);
    }

    /**
     * Handles user login requests by delegating authentication to the Gateway service layer.
     *
     * <p>
     * The service layer performs the downstream call to the User-Service,
     * processes the response, and manages session establishment (e.g. cookie handling).
     * </p>
     *
     * @param request  The login credentials (email/password)
     * @param exchange The current server exchange context
     * @return A successful authentication response containing user data
     */
    @PostMapping("/signup")
    public Mono<ResponseEntity<ApiResponse<SuccessfulLoginResponse>>> signup(
            @RequestBody SignupRequest request,
            ServerWebExchange exchange) {

        return userGatewayService.signup(request, exchange)
                .map(ResponseEntity::ok);
    }

    /**
     * Logs the user out by clearing the JWT HttpOnly cookie.
     * No downstream call is needed — the session is stateless (JWT).
     *
     * @param exchange The current server exchange context
     * @return 204 No Content on success
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(ServerWebExchange exchange) {
        return cookieUtil.clearTokenCookie(exchange)
                .then(Mono.just(ResponseEntity.<Void>status(HttpStatus.NO_CONTENT).build()));
    }
}