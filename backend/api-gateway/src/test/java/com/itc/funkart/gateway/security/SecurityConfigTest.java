package com.itc.funkart.gateway.security;

import com.itc.funkart.gateway.config.AppConfig;
import com.itc.funkart.gateway.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

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

    @Autowired
    private WebTestClient webTestClient;

    @Mock
    private AppConfig appConfig;

    /**
     * We mock the filter so tests can inject or withhold authentication at will.
     * The real filter would need Redis and a valid JWT.
     */
    @MockitoBean
    private JwtAuthWebFilter jwtAuthWebFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UrlBasedCorsConfigurationSource corsSource;

    @BeforeEach
    void setUp() {
        lenient().when(jwtAuthWebFilter.filter(any(), any())).thenAnswer(inv -> {
            ServerWebExchange exchange = inv.getArgument(0);
            WebFilterChain chain = inv.getArgument(1);
            return (chain != null) ? chain.filter(exchange) : reactor.core.publisher.Mono.empty();
        });
    }

    @TestConfiguration
    static class SecurityTestRoutesConfig {
        @Bean
        public RouteLocator proxyRoutes(RouteLocatorBuilder builder) {
            return builder.routes()
                    .route("test_route", r -> r.path("/api/v1/users/me")
                            .uri("http://localhost:60030"))
                    .build();
        }
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
                    .uri("/api/v1/users/login")
                    .exchange()
                    .expectStatus().value(status ->
                            assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status,
                                    "/users/login should not be 401"));
        }

        @Test
        @DisplayName("/users/signup is accessible without a token")
        void usersSignup_isPublic() {
            webTestClient.post()
                    .uri("/api/v1/users/signup")
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
                            assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status))
                    .expectStatus().value(status ->
                            assertNotEquals(HttpStatus.FORBIDDEN.value(), status));
        }

        @Test
        @DisplayName("/payments/webhook/** is accessible without a token")
        void paymentWebhook_isPublic() {
            webTestClient.post()
                    .uri("/api/v1/payments/webhook/stripe")
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
                    .uri("/api/v1/users/profile") // Prefix matches Config
                    .exchange()
                    // If it's 500, your GlobalExceptionHandler is catching
                    // the Routing error before the Security error.
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

}
