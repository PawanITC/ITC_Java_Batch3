package com.itc.funkart.gateway.controller;

import com.itc.funkart.gateway.dto.request.LoginRequest;
import com.itc.funkart.gateway.dto.request.SignupRequest;
import com.itc.funkart.gateway.security.CookieUtil;
import com.itc.funkart.gateway.security.JwtTokenValidator;
import com.itc.funkart.gateway.service.GithubOAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration tests for {@link UserController} using WireMock to simulate downstream microservices.
 * <p>
 * This suite validates the full reactive flow of the Gateway's local endpoints, ensuring that
 * incoming requests are correctly processed, forwarded to the User Service, and that
 * resulting JWTs are stored as secure cookies.
 * </p>
 * * @see UserController
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class UserControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private GithubOAuthService githubOAuthService;

    /**
     * Mocked utility to verify cookie placement without affecting actual browser state.
     * Updated to {@link MockitoBean} for Spring Boot 3.5.x compatibility.
     */
    @MockitoBean
    private CookieUtil cookieUtil;

    /**
     * Mocked validator to satisfy dependency requirements for the Gateway's security filters
     * during the integration test context startup.
     */
    @MockitoBean
    private JwtTokenValidator jwtTokenValidator;

    @BeforeEach
    void setUp() {
        reset(cookieUtil, jwtTokenValidator, githubOAuthService);
    }

    /**
     * Reusable JSON response fragment representing a successful identity token
     * returned by the downstream User Service.
     */
    private final String SUCCESS_JSON = """
    { 
        "data": { 
            "token": "fake-jwt-token",
            "user": { "id": 1, "name": "Test User", "email": "test@example.com", "role": "ROLE_USER" }
        } 
    }
    """;

    /**
     * Tests the login flow.
     * <p>
     * Verifies that:
     * 1. The Gateway sends a request to {@code /api/v1/users/login} on the downstream service.
     * 2. The {@link CookieUtil} is invoked to set the JWT cookie upon a successful response.
     * 3. The Gateway returns a 200 OK status to the frontend.
     * </p>
     */
    @Test
    @DisplayName("Login: Success sets cookie and returns 200")
    void login_Success() {
        // Stub the downstream User Service (WireMock)
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

    @Test
    @DisplayName("Login: Downstream 401 Unauthorized should propagate")
    void login_DownstreamError() {
        // Stub WireMock to return an error
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
                .expectStatus().isUnauthorized(); // Validates error propagation

        // Verify cookie was NEVER added because doOnNext shouldn't fire
        verify(cookieUtil, times(0)).addTokenCookie(any(), any(), any());
    }

    /**
     * Tests the signup flow.
     * <p>
     * Ensures that registration requests result in the same secure cookie behavior
     * as the login process after successfully communicating with the User Service at
     * {@code /api/v1/users/signup}.
     * </p>
     */
    @Test
    @DisplayName("Signup: Success sets cookie and returns 200")
    void signup_Success() {
        stubFor(post(urlEqualTo("/api/v1/users/signup"))
                .willReturn(okJson(SUCCESS_JSON)));

        // MUST use /api/v1
        webTestClient.post()
                .uri("/api/v1/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new SignupRequest("Test User","test@example.com", "password"))
                .exchange()
                .expectStatus().isOk();

        verify(cookieUtil).addTokenCookie(any(), eq("fake-jwt-token"), any());
    }
}