package com.itc.funkart.gateway.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Verification of the Gateway Security Perimeter.
 * * Ensures public endpoints (webhooks, auth) are accessible without tokens
 * and protected endpoints (user data, etc.) are strictly guarded.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(SecurityConfig.class)
public class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;


    /**
     * Verifies that the Stripe Webhook endpoint is accessible without a JWT.
     * <p>
     * <b>Note:</b> This test accepts 404/500 statuses as a "Pass" because it proves
     * the security filter allowed the request through. The failure occurs at the
     * routing layer (since the downstream payment-service is not running),
     * not the security layer.
     */
    @Test
    @DisplayName("Public Webhook: Should bypass security filters")
    void whenAccessPublicEndpoint_thenSucceeds() {
        // We verify that the 'Bouncer' (SecurityConfig) allows the request.
        // We accept 404 or 500 because the downstream payment-service
        // isn't running, but a 401/403 would mean the security filter failed.
        webTestClient.post()
                .uri("/api/v1/payments/webhook")
                .exchange()
                .expectStatus().value(status -> {
                    boolean isPermitted = status != 401 && status != 403;
                    if (!isPermitted) {
                        throw new AssertionError("Security filter blocked a public route! Status: " + status);
                    }
                });
    }

    /**
     * Verifies that internal user endpoints are protected by the Security Filter.
     * <p>
     * Expects a 401 Unauthorized status when no authentication credentials
     * (cookies/headers) are provided.
     */
    @Test
    @DisplayName("Protected Profile: Should be blocked without JWT")
    void whenAccessProtectedEndpoint_thenUnauthorized() {
        webTestClient.get()
                .uri("/api/v1/users/profile")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}