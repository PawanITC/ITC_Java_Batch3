package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.props.ApiProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * <h2>Security Infrastructure Integration Tests</h2>
 * <p>
 * Validates the {@link SecurityConfig} rules by hitting the gateway's filter chain.
 * Uses a "Senior" status assertion strategy: if a request passes security but fails
 * with 404/500, it confirms the security gate is open for that route.
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityConfigTest {

    private WebTestClient webTestClient;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ApiProperties apiProperties;

    @MockitoBean
    private JwtWebFilter jwtWebFilter;

    // These don't affect path matching, so keeping them as mocks is fine
    @MockitoBean
    private CookieUtil cookieUtil;
    @MockitoBean
    private JwtTokenValidator jwtTokenValidator;

    /**
     * Rebinds the WebTestClient to the application context to ensure the full
     * security filter chain is engaged during tests.
     */
    @BeforeEach
    void setUp() {
        this.webTestClient = WebTestClient.bindToApplicationContext(context)
                .configureClient()
                .build();

        // 1. Transparent & Authenticating Filter Mock
        when(jwtWebFilter.filter(any(), any())).thenAnswer(invocation -> {
            ServerWebExchange exchange = invocation.getArgument(0);
            WebFilterChain chain = invocation.getArgument(1);

            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            // Check if the test is sending a token
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // Create a fake authentication object
                var auth = new UsernamePasswordAuthenticationToken(
                        "123",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

                // IMPORTANT: Pass the request forward AND inject the security context
                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
            }

            // If no token, just pass through (for public endpoints)
            return chain.filter(exchange);
        });
    }

    /**
     * Ensures that a valid JWT token allows the request to proceed beyond the security layer.
     */
    @Test
    @DisplayName("Authenticated Request: Should allow access with valid token")
    void validToken_shouldAllowAccess() {
        String token = "valid-token";
        Claims claims = Jwts.claims()
                .subject("123")
                .add("role", "ROLE_USER")
                .build();

        when(jwtTokenValidator.validateAndParseClaims(token)).thenReturn(claims);

        webTestClient.get()
                .uri("/api/v1/users/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().value(status ->
                        assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status));
    }

    /**
     * Validates that payment webhooks are excluded from authentication requirements.
     */
    @Test
    @DisplayName("Public endpoint (Webhook): Should pass security layer")
    void publicWebhook_shouldPassSecurity() {
        String version = apiProperties.version(); // Get real version (e.g. /api/v1)

        webTestClient.post()
                .uri(version + "/payments/webhook/test")
                .exchange()
                .expectStatus().value(status -> {
                    // We expect 404/500/200, but NOT 401/403
                    assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status, "Should not be 401");
                });
    }

    /**
     * Validates that login endpoints are accessible to unauthenticated users.
     */
    @Test
    @DisplayName("Auth endpoints: Should be public")
    void publicAuth_shouldPassSecurity() {
        String version = apiProperties.version();

        webTestClient.post()
                .uri(version + "/users/login")
                .exchange()
                .expectStatus().value(status ->
                        assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status, "Should not be 401"));
    }

    /**
     * Confirms that requests to protected resources without credentials are rejected with 401.
     */
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