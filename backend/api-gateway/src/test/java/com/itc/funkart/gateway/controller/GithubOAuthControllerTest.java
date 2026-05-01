package com.itc.funkart.gateway.controller;

import com.itc.funkart.gateway.dto.UserDto;
import com.itc.funkart.gateway.dto.response.SuccessfulLoginResponse;
import com.itc.funkart.gateway.exception.JwtAuthenticationException;
import com.itc.funkart.gateway.exception.OAuthException;
import com.itc.funkart.gateway.response.ApiResponse;
import com.itc.funkart.gateway.security.JwtAuthWebFilter;
import com.itc.funkart.gateway.service.JwtService;
import com.itc.funkart.gateway.service.OAuthGatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * <h2>GithubOAuthController — Web Layer Tests</h2>
 *
 * <p>Verifies HTTP-level behavior of {@link GithubOAuthController} in isolation.
 * All downstream logic lives in {@link OAuthGatewayService}, which is mocked here.
 *
 * <p><b>Endpoints under test:</b>
 * <ul>
 *   <li>{@code GET /oauth/github/login}    — redirects to GitHub (307)</li>
 *   <li>{@code GET /oauth/github/callback} — exchanges code, sets cookie, redirects (307)</li>
 *   <li>{@code GET /oauth/github/logout}   — clears cookie, returns 204</li>
 *   <li>{@code POST /oauth/github/refresh} — rotates access + refresh tokens (200)</li>
 * </ul>
 *
 * <p>Security is fully disabled via {@link MockSecurityConfig} so tests focus
 * exclusively on routing and service delegation.
 */
@WebFluxTest(controllers = GithubOAuthController.class)
@ActiveProfiles("test")
@Import(GithubOAuthControllerTest.MockSecurityConfig.class)
class GithubOAuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    /**
     * The sole collaborator of this controller.
     */
    @MockitoBean
    private OAuthGatewayService oAuthGatewayService;


    /**
     * The {@link JwtAuthWebFilter} must be provided so the WebFlux security
     * autoconfiguration can satisfy its dependency, even though security
     * is permit-all in tests.
     */
    @MockitoBean
    private JwtAuthWebFilter jwtAuthWebFilter;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Use the robust pass-through to avoid NPEs
        lenient().when(jwtAuthWebFilter.filter(any(), any())).thenAnswer(inv -> {
            ServerWebExchange exchange = inv.getArgument(0);
            WebFilterChain chain = inv.getArgument(1);
            return (chain != null) ? chain.filter(exchange) : Mono.empty();
        });
    }

    /**
     * Disables all security rules and stubs the JWT filter so it transparently
     * passes every request through — keeping tests focused on the controller layer.
     */
    @TestConfiguration
    static class MockSecurityConfig {
        @Bean
        @Primary
        public SecurityWebFilterChain testSecurityChain(ServerHttpSecurity http) {
            return http
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .authorizeExchange(ex -> ex.anyExchange().permitAll())
                    .build();
        }
    }


    // -------------------------------------------------------------------------
    // GET /api/v1/oauth/github/login
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /api/v1/oauth/github/login")
    class LoginTests {
        @Test
        @DisplayName("Returns 307 Redirect to GitHub")
        void login_returns307() {
            String githubUrl = "https://github.com/login/oauth/authorize?client_id=test";
            when(oAuthGatewayService.buildGithubRedirectUrl()).thenReturn(githubUrl);

            webTestClient.get()
                    .uri("/api/v1/oauth/github/login")
                    .exchange()
                    .expectStatus().isTemporaryRedirect()
                    .expectHeader().location(githubUrl);
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/oauth/github/callback
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /api/v1/oauth/github/callback")
    class CallbackTests {
        @Test
        @DisplayName("Redirects to frontend on successful exchange")
        void callback_redirectsToFrontend() {
            String frontendUrl = "http://localhost:5173";
            when(oAuthGatewayService.handleCallback(anyString(), any())).thenReturn(Mono.empty());
            when(oAuthGatewayService.frontendRedirect()).thenReturn(frontendUrl);

            webTestClient.get()
                    .uri(u -> u.path("/api/v1/oauth/github/callback").queryParam("code", "auth-code").build())
                    .exchange()
                    .expectStatus().isTemporaryRedirect()
                    .expectHeader().location(frontendUrl);
        }

        @Test
        @DisplayName("Returns 401 when handleCallback fails")
        void callback_returns401OnError() {
            when(oAuthGatewayService.handleCallback(anyString(), any()))
                    .thenReturn(Mono.error(new OAuthException("GitHub error")));

            webTestClient.get()
                    .uri(u -> u.path("/api/v1/oauth/github/callback").queryParam("code", "bad").build())
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectBody()
                    .jsonPath("$.error.code").isEqualTo("OAUTH_ERROR");
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/oauth/github/refresh
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /api/v1/oauth/github/refresh")
    class RefreshTests {
        @Test
        @DisplayName("Returns 200 with new tokens")
        void refresh_success() {
            UserDto user = new UserDto(1L, "Alice", "alice@example.com", "ROLE_USER");
            SuccessfulLoginResponse resp = new SuccessfulLoginResponse(user, "new-token");
            ApiResponse<SuccessfulLoginResponse> apiResp = new ApiResponse<>(resp, "Refreshed");

            when(oAuthGatewayService.refresh(anyString(), any())).thenReturn(Mono.just(apiResp));

            webTestClient.post()
                    .uri("/api/v1/oauth/github/refresh")
                    .cookie("refresh_token", "valid-token")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.data.token").isEqualTo("new-token");
        }

        @Test
        @DisplayName("Returns 401 on invalid refresh token")
        void refresh_invalidToken() {
            when(oAuthGatewayService.refresh(anyString(), any()))
                    .thenReturn(Mono.error(new JwtAuthenticationException("Invalid refresh token")));

            webTestClient.post()
                    .uri("/api/v1/oauth/github/refresh")
                    .cookie("refresh_token", "expired-token")
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectBody()
                    .jsonPath("$.error.code").isEqualTo("AUTH_ERROR")
                    .jsonPath("$.error.message").isEqualTo("Invalid refresh token");
        }
    }
}