package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityConfigTest {

    private WebTestClient webTestClient; // Remove @Autowired

    @Autowired
    private ApplicationContext context; // Inject the context

    @MockitoBean
    private AppConfig appConfig;
    @MockitoBean
    private CookieUtil cookieUtil;
    @MockitoBean
    private JwtTokenValidator jwtTokenValidator;

    @BeforeEach
    void setUp() {
        // Manually bind to context to bypass the network/Jenkins proxy
        this.webTestClient = WebTestClient.bindToApplicationContext(context)
                .configureClient()
                .build();

        when(appConfig.frontendUrl()).thenReturn("http://localhost:5173");
    }

    @Test
    @DisplayName("Public endpoint (Webhook): Should pass security layer")
    void publicWebhook_shouldPassSecurity() {
        webTestClient.mutateWith(csrf()).post()
                .uri("/payments/webhook")
                .exchange()
                .expectStatus()
                .value(status -> {
                    // If it's 404 or 500, security let us through.
                    // We ONLY fail if it's 401 (Unauthorized) or 403 (Forbidden).
                    if (status == 401 || status == 403) {
                        throw new AssertionError("Security blocked the webhook: " + status);
                    }
                });
    }

    @Test
    @DisplayName("Auth endpoints: Should be public")
    void publicAuth_shouldPassSecurity() {
        webTestClient.mutateWith(csrf())
                .post()
                .uri("/api/v1/users/login")
                .exchange()
                .expectStatus()
                .value(status -> {
                    // If we get 401 or 403, Security is the problem.
                    // If we get 404 (No resource) or 500 (Connection refused),
                    // Security SUCCEEDED in letting us through the gate.
                    if (status == 401 || status == 403) {
                        throw new AssertionError("Security blocked the request with status: " + status);
                    }
                });
    }

    @Test
    @DisplayName("Protected endpoint: Must be blocked without JWT")
    void protectedEndpoint_shouldBeUnauthorized() {
        webTestClient.get()
                .uri("/api/v1/users/profile")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }
}