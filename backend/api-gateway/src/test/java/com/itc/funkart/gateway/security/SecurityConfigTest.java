package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;

/**
 * Verification of the Gateway Security Perimeter.
 * Ensures public endpoints (webhooks, auth) are accessible without tokens
 * and protected endpoints (user data, etc.) are strictly guarded.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    private AppConfig appConfig;

    @MockitoBean
    private CookieUtil cookieUtil;

    @MockitoBean
    private JwtTokenValidator jwtTokenValidator;

    @MockitoBean
    private JwtWebFilter jwtWebFilter;

    @BeforeEach
    void setUp() {
        // Fix 2: Clean the slate for the global filter
        reset(appConfig, cookieUtil, jwtTokenValidator, jwtWebFilter);

        when(appConfig.api().version()).thenReturn("/api/v1");

        // Re-establish the pass-through logic
        when(jwtWebFilter.filter(any(), any()))
                .thenAnswer(invocation -> {
                    ServerWebExchange exchange = invocation.getArgument(0);
                    WebFilterChain chain = invocation.getArgument(1);
                    return chain.filter(exchange);
                });
    }

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
        webTestClient.post()
                .uri("/payments/webhook")
                .exchange()
                .expectStatus().value(status -> {
                    // SUCCESS condition: The bouncer let us in.
                    // We expect 404 because no real payment-service is running in this test,
                    // but 401/403 means the bouncer stopped us at the door.
                    if (status == 401 || status == 403) {
                        throw new AssertionError("Public route was blocked! Status: " + status);
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