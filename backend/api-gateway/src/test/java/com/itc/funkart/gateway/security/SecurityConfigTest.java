package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * <h2>Security Configuration — Integration Tests</h2>
 *
 * <p>Boots the full Spring application context (test profile) and drives
 * requests through the real security filter chain to verify authorization rules.
 *
 * <p><b>Strategy:</b> a mocked {@link JwtAuthWebFilter} lets individual tests
 * decide whether a request carries a valid identity or not, while keeping Redis,
 * downstream services, and real JWT signing out of the picture.
 *
 * <p><b>Rules verified:</b>
 * <ul>
 *   <li>Public endpoints ({@code /users/login}, {@code /users/signup},
 *       {@code /actuator/**}, {@code /payments/webhook/**}) must <em>not</em> return 401/403</li>
 *   <li>All other paths must return 401 without a valid token</li>
 *   <li>A synthetic authenticated token allows access past the security layer</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityConfigTest {

    private WebTestClient webTestClient;

    @Autowired
    private ApplicationContext context;

    /**
     * We mock the filter so tests can inject or withhold authentication at will.
     * The real filter would need Redis and a valid JWT.
     */
    @MockitoBean
    private JwtAuthWebFilter jwtAuthWebFilter;

    @BeforeEach
    void setUp() {
        this.webTestClient = WebTestClient.bindToApplicationContext(context)
                .configureClient()
                .build();

        // Default behaviour: if the request carries a Bearer token, inject authentication;
        // otherwise pass through transparently (let the security rules do their job).
        when(jwtAuthWebFilter.filter(any(), any())).thenAnswer(invocation -> {
            ServerWebExchange exchange = invocation.getArgument(0);
            WebFilterChain chain = invocation.getArgument(1);

            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                var auth = new UsernamePasswordAuthenticationToken(
                        new UserDto(1L, "Test", "test@test.com", "ROLE_USER"),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
            }

            return chain.filter(exchange);
        });
    }

    // -------------------------------------------------------------------------
    // Public endpoints — must NOT return 401 or 403
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Public endpoints")
    class PublicEndpointTests {

        @Test
        @DisplayName("/users/login is accessible without a token")
        void usersLogin_isPublic() {
            webTestClient.post()
                    .uri("/users/login")
                    .exchange()
                    .expectStatus().value(status ->
                            assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status,
                                    "/users/login should not be 401"));
        }

        @Test
        @DisplayName("/users/signup is accessible without a token")
        void usersSignup_isPublic() {
            webTestClient.post()
                    .uri("/users/signup")
                    .exchange()
                    .expectStatus().value(status ->
                            assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status,
                                    "/users/signup should not be 401"));
        }

        @Test
        @DisplayName("/actuator/health is accessible without a token")
        void actuatorHealth_isPublic() {
            webTestClient.get()
                    .uri("/actuator/health")
                    .exchange()
                    .expectStatus().value(status ->
                            assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status,
                                    "/actuator/health should not be 401"));
        }

        @Test
        @DisplayName("/payments/webhook/** is accessible without a token")
        void paymentWebhook_isPublic() {
            webTestClient.post()
                    .uri("/payments/webhook/stripe")
                    .exchange()
                    .expectStatus().value(status ->
                            assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status,
                                    "Payment webhook should not be 401"));
        }

        @Test
        @DisplayName("OPTIONS preflight requests are permitted")
        void optionsPreflight_isPermitted() {
            webTestClient.options()
                    .uri("/api/v1/orders/1")
                    .exchange()
                    .expectStatus().value(status ->
                            assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status,
                                    "OPTIONS should not be 401"));
        }
    }

    // -------------------------------------------------------------------------
    // Protected endpoints — must return 401 without credentials
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Protected endpoints")
    class ProtectedEndpointTests {

        @Test
        @DisplayName("GET /api/v1/users/profile returns 401 without a token")
        void usersProfile_requires401WithoutToken() {
            webTestClient.get()
                    .uri("/api/v1/users/profile")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("GET /api/v1/orders returns 401 without a token")
        void orders_requires401WithoutToken() {
            webTestClient.get()
                    .uri("/api/v1/orders")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    // -------------------------------------------------------------------------
    // Authenticated access — must NOT return 401
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Authenticated access")
    class AuthenticatedAccessTests {

        @Test
        @DisplayName("Authenticated request passes security layer (may 404 but not 401)")
        void authenticatedRequest_passesSecurityGate() {
            webTestClient.get()
                    .uri("/api/v1/users/me")
                    .header("Authorization", "Bearer synthetic-token")
                    .exchange()
                    .expectStatus().value(status ->
                            assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status,
                                    "Authenticated request should not be 401"));
        }
    }
}
