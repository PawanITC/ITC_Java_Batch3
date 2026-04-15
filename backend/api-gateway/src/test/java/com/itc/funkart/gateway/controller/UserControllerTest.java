package com.itc.funkart.gateway.controller;

import com.itc.funkart.gateway.dto.request.LoginRequest;
import com.itc.funkart.gateway.dto.request.SignupRequest;
import com.itc.funkart.gateway.security.CookieUtil;
import com.itc.funkart.gateway.security.JwtTokenValidator;
import com.itc.funkart.gateway.security.JwtWebFilter;
import com.itc.funkart.gateway.security.SecurityConfig;
import com.itc.funkart.gateway.service.GithubOAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>UserController Integration Test</h2>
 * <p>
 * Validates the Gateway's internal UserController authentication flow.
 * Since the Gateway uses a global base-path (/api/v1), this test calls the
 * full versioned URIs while verifying that the controller properly communicates
 * with the downstream User Service via WireMock.
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureWireMock(port = 0)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class UserControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private GithubOAuthService githubOAuthService;

    @MockitoBean
    private CookieUtil cookieUtil;

    @MockitoBean
    private JwtTokenValidator jwtTokenValidator;

    @MockitoBean
    private JwtWebFilter jwtWebFilter;

    /**
     * Resets mocks and configures security bypass.
     */
    @BeforeEach
    void setUp() {
        reset(cookieUtil, jwtTokenValidator, githubOAuthService, jwtWebFilter);

        // Force Security Pass-through (bypass JWT validation)
        when(jwtWebFilter.filter(any(ServerWebExchange.class), any(WebFilterChain.class)))
                .thenAnswer(invocation -> {
                    ServerWebExchange exchange = invocation.getArgument(0);
                    WebFilterChain chain = invocation.getArgument(1);
                    return chain.filter(exchange);
                });
    }

    private final String SUCCESS_JSON = """
    { 
        "data": { 
            "token": "fake-jwt-token",
            "user": { "id": 1, "name": "Test User", "email": "test@example.com", "role": "ROLE_USER" }
        } 
    }
    """;

    /**
     * Verifies that the login endpoint is reachable via the versioned path and
     * successfully triggers JWT cookie creation.
     */
    @Test
    @DisplayName("Login: Success sets cookie and returns 200")
    void login_Success() {
        stubFor(post(urlEqualTo("/api/v1/users/login"))
                .willReturn(okJson(SUCCESS_JSON)));

        webTestClient.post()
                .uri("/api/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("test@example.com", "password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.token").isEqualTo("fake-jwt-token");

        verify(cookieUtil).addTokenCookie(any(), eq("fake-jwt-token"), any());
    }

    /**
     * Validates error propagation when the downstream service returns 401.
     */
    @Test
    @DisplayName("Login: Downstream 401 Unauthorized should propagate")
    void login_DownstreamError() {
        stubFor(post(urlEqualTo("/api/v1/users/login"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Invalid credentials\"}")));

        webTestClient.post()
                .uri("/api/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("wrong@email.com", "wrong-pass"))
                .exchange()
                .expectStatus().isUnauthorized();

        verify(cookieUtil, never()).addTokenCookie(any(), any(), any());
    }

    /**
     * Validates that the signup process correctly proxies to the User Service.
     */
    @Test
    @DisplayName("Signup: Success sets cookie and returns 200")
    void signup_Success() {
        stubFor(post(urlEqualTo("/api/v1/users/signup"))
                .willReturn(okJson(SUCCESS_JSON)));

        webTestClient.post()
                .uri("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new SignupRequest("Test User","test@example.com", "password"))
                .exchange()
                .expectStatus().isOk();

        verify(cookieUtil).addTokenCookie(any(), eq("fake-jwt-token"), any());
    }
}