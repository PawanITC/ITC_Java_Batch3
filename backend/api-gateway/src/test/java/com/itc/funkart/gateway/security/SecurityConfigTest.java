package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.Mockito.when;

/**
 * Verification of the Gateway Security Perimeter.
 * Ensures public endpoints (webhooks, auth) are accessible without tokens
 * and protected endpoints (user data, etc.) are strictly guarded.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AppConfig appConfig;

    @MockitoBean
    private CookieUtil cookieUtil;

    @MockitoBean
    private JwtTokenValidator jwtTokenValidator;

    @BeforeEach
    void setUp() {
        when(appConfig.frontendUrl()).thenReturn("http://localhost:5173");
    }

    /**
     * Verifies that public endpoints are NOT blocked by security filters.
     * We only assert that the request is NOT rejected with 401/403.
     * Any other status (404/405) means it passed security layer successfully.
     */
    @Test
    @DisplayName("Public endpoint should not be blocked by security")
    void whenAccessPublicEndpoint_thenNotUnauthorizedOrForbidden() {

        webTestClient.post()
                .uri("/payments/webhook")
                .exchange()
                .expectStatus()
                .value(status -> {
                    if (status == 401 || status == 403) {
                        throw new AssertionError("Public endpoint incorrectly blocked by security");
                    }
                });
    }

    /**
     * Ensures protected endpoints require authentication.
     */
    @Test
    @DisplayName("Protected endpoint should require authentication")
    void whenAccessProtectedEndpoint_thenUnauthorized() {

        webTestClient.get()
                .uri("/api/v1/users/profile")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }
}