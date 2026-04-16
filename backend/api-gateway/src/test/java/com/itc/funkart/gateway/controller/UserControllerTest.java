package com.itc.funkart.gateway.controller;

import com.itc.funkart.gateway.dto.request.LoginRequest;
import com.itc.funkart.gateway.dto.request.SignupRequest;
import com.itc.funkart.gateway.security.CookieUtil;
import com.itc.funkart.gateway.security.JwtTokenValidator;
import com.itc.funkart.gateway.service.GithubOAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>UserController Integration Test</h2>
 *
 * <p>
 * This test validates the Gateway authentication proxy layer for user login and signup.
 * It verifies:
 * <ul>
 *     <li>Request routing to downstream User Service (via WireMock)</li>
 *     <li>JWT token handling via CookieUtil</li>
 *     <li>Proper JSON response propagation</li>
 * </ul>
 *
 * <p>
 * Security is explicitly overridden in this test context using a permissive
 * SecurityWebFilterChain to avoid interference from JWT authentication filters.
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(UserControllerTest.TestSecurityConfig.class)
public class UserControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private GithubOAuthService githubOAuthService;

    @MockitoBean
    private CookieUtil cookieUtil;

    @MockitoBean
    private JwtTokenValidator jwtTokenValidator;

    /**
     * Test-only security configuration.
     *
     * <p>
     * Disables authentication entirely so that tests focus only on:
     * controller logic + downstream service integration.
     * </p>
     */
    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        SecurityWebFilterChain testSecurityChain(ServerHttpSecurity http) {
            return http
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                    .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                    .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                    .build();
        }
    }

    private final String SUCCESS_JSON = """
        {
            "data": {
                "token": "fake-jwt-token",
                "user": {
                    "id": 1,
                    "name": "Test User",
                    "email": "test@example.com",
                    "role": "ROLE_USER"
                }
            }
        }
        """;

    /**
     * <h3>Login Flow - Success Case</h3>
     *
     * <p>
     * Ensures that a valid login request is:
     * <ul>
     *     <li>Forwarded to the User Service</li>
     *     <li>Properly parsed</li>
     *     <li>Returns JWT token in response body</li>
     *     <li>Triggers cookie creation</li>
     * </ul>
     * </p>
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
     * <h3>Login Flow - Downstream Failure</h3>
     *
     * <p>
     * Ensures that authentication errors from the User Service
     * are correctly propagated through the gateway without transformation.
     * </p>
     */
    @Test
    @DisplayName("Login: Downstream 401 Unauthorized should propagate")
    void login_DownstreamError() {

        stubFor(post(urlEqualTo("/api/v1/users/login"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\":\"Invalid credentials\"}")));

        webTestClient.post()
                .uri("/api/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("wrong@email.com", "wrong-pass"))
                .exchange()
                .expectStatus().isUnauthorized();

        verify(cookieUtil, never()).addTokenCookie(any(), any(), any());
    }

    /**
     * <h3>Signup Flow - Success Case</h3>
     *
     * <p>
     * Ensures signup correctly proxies to the User Service and
     * triggers JWT cookie creation.
     * </p>
     */
    @Test
    @DisplayName("Signup: Success sets cookie and returns 200")
    void signup_Success() {

        stubFor(post(urlEqualTo("/api/v1/users/signup"))
                .willReturn(okJson(SUCCESS_JSON)));

        webTestClient.post()
                .uri("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new SignupRequest("Test User", "test@example.com", "password"))
                .exchange()
                .expectStatus().isOk();

        verify(cookieUtil).addTokenCookie(any(), eq("fake-jwt-token"), any());
    }
}