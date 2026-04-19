package com.itc.funkart.gateway.controller;

import com.itc.funkart.gateway.dto.request.LoginRequest;
import com.itc.funkart.gateway.dto.request.SignupRequest;
import com.itc.funkart.gateway.dto.response.SuccessfulLoginResponse;
import com.itc.funkart.gateway.response.ApiResponse;
import com.itc.funkart.gateway.service.UserGatewayService;
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
@RequestMapping("/users")
public class UserController {

    private final UserGatewayService userGatewayService;

    /**
     * @param userGatewayService Service responsible for handling authentication
     *                           orchestration and communication with the User-Service.
     */
    public UserController(UserGatewayService userGatewayService) {
        this.userGatewayService = userGatewayService;
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
}