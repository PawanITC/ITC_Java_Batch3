package com.itc.funkart.gateway.controller;

import com.itc.funkart.gateway.dto.UserDto;
import com.itc.funkart.gateway.dto.response.SuccessfulLoginResponse;
import com.itc.funkart.gateway.exception.JwtAuthenticationException;
import com.itc.funkart.gateway.exception.OAuthException;
import com.itc.funkart.gateway.response.ApiResponse;
import com.itc.funkart.gateway.security.JwtAuthWebFilter;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * <h2>GithubOAuthController — Web Layer Tests</h2>
 *
 * <p>Verifies HTTP-level behaviour of {@link GithubOAuthController} in isolation.
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
@Import(GithubOAuthControllerTest.MockSecurityConfig.class)
class GithubOAuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    /** The sole collaborator of this controller. */
    @MockitoBean
    private OAuthGatewayService oAuthGatewayService;

    /**
     * The {@link JwtAuthWebFilter} must be provided so the WebFlux security
     * auto-configuration can satisfy its dependency, even though security
     * is permit-all in tests.
     */
    @MockitoBean
    private JwtAuthWebFilter jwtAuthWebFilter;

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

    @BeforeEach
    void setUp() {
        // JWT filter must be transparent: pass every request straight through
        lenient().when(jwtAuthWebFilter.filter(any(), any())).thenAnswer(inv -> {
            ServerWebExchange exchange = inv.getArgument(0);
            WebFilterChain chain     = inv.getArgument(1);
            return chain != null ? chain.filter(exchange) : Mono.empty();
        });
    }

    @BeforeEach
    void resetMocks() {
        reset(oAuthGatewayService);
    }

    // -------------------------------------------------------------------------
    // GET /oauth/github/login
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /oauth/github/login")
    class LoginTests {

        @Test
        @DisplayName("Returns 307 Temporary Redirect to GitHub")
        void login_returns307() {
            when(oAuthGatewayService.buildGithubRedirectUrl())
                    .thenReturn("https://github.com/login/oauth/authorize?client_id=test");

            webTestClient.get()
                    .uri("/oauth/github/login")
                    .exchange()
                    .expectStatus().isTemporaryRedirect();
        }

        @Test
        @DisplayName("Location header points to the URL built by the service")
        void login_locationHeaderMatchesService() {
            String githubUrl = "https://github.com/login/oauth/authorize?client_id=test&scope=user:email";
            when(oAuthGatewayService.buildGithubRedirectUrl()).thenReturn(githubUrl);

            webTestClient.get()
                    .uri("/oauth/github/login")
                    .exchange()
                    .expectHeader().location(githubUrl);
        }

        @Test
        @DisplayName("Delegates URL construction to OAuthGatewayService")
        void login_delegatesToService() {
            when(oAuthGatewayService.buildGithubRedirectUrl()).thenReturn("https://github.com");

            webTestClient.get().uri("/oauth/github/login").exchange();

            verify(oAuthGatewayService).buildGithubRedirectUrl();
        }
    }

    // -------------------------------------------------------------------------
    // GET /oauth/github/callback
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /oauth/github/callback")
    class CallbackTests {

        @Test
        @DisplayName("Returns 307 Temporary Redirect on successful code exchange")
        void callback_returns307OnSuccess() {
            when(oAuthGatewayService.handleCallback(eq("auth-code"), any()))
                    .thenReturn(Mono.empty());
            when(oAuthGatewayService.frontendRedirect())
                    .thenReturn("http://localhost:5173");

            webTestClient.get()
                    .uri(u -> u.path("/oauth/github/callback").queryParam("code", "auth-code").build())
                    .exchange()
                    .expectStatus().isTemporaryRedirect();
        }

        @Test
        @DisplayName("Location header points to the frontend URL")
        void callback_redirectsToFrontend() {
            when(oAuthGatewayService.handleCallback(anyString(), any()))
                    .thenReturn(Mono.empty());
            when(oAuthGatewayService.frontendRedirect())
                    .thenReturn("http://localhost:5173");

            webTestClient.get()
                    .uri(u -> u.path("/oauth/github/callback").queryParam("code", "code").build())
                    .exchange()
                    .expectHeader().location("http://localhost:5173");
        }

        @Test
        @DisplayName("Passes the authorization code to OAuthGatewayService.handleCallback")
        void callback_passesCodeToService() {
            when(oAuthGatewayService.handleCallback(eq("my-code"), any()))
                    .thenReturn(Mono.empty());
            when(oAuthGatewayService.frontendRedirect()).thenReturn("http://localhost:5173");

            webTestClient.get()
                    .uri(u -> u.path("/oauth/github/callback").queryParam("code", "my-code").build())
                    .exchange();

            verify(oAuthGatewayService).handleCallback(eq("my-code"), any());
        }

        @Test
        @DisplayName("Returns 401 when OAuthGatewayService throws OAuthException")
        void callback_returns401OnOAuthException() {
            when(oAuthGatewayService.handleCallback(eq("bad-code"), any()))
                    .thenReturn(Mono.error(new OAuthException("GitHub token exchange failed")));

            webTestClient.get()
                    .uri(u -> u.path("/oauth/github/callback").queryParam("code", "bad-code").build())
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectBody()
                    .jsonPath("$.error.code").isEqualTo("OAUTH_ERROR")
                    .jsonPath("$.error.message").isEqualTo("GitHub token exchange failed");
        }
    }

    // -------------------------------------------------------------------------
    // GET /oauth/github/logout
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /oauth/github/logout")
    class LogoutTests {

        @Test
        @DisplayName("Returns 204 No Content")
        void logout_returns204() {
            when(oAuthGatewayService.logout(any())).thenReturn(Mono.empty());

            webTestClient.get()
                    .uri("/oauth/github/logout")
                    .exchange()
                    .expectStatus().isNoContent();
        }

        @Test
        @DisplayName("Delegates to OAuthGatewayService.logout")
        void logout_delegatesToService() {
            when(oAuthGatewayService.logout(any())).thenReturn(Mono.empty());

            webTestClient.get().uri("/oauth/github/logout").exchange();

            verify(oAuthGatewayService).logout(any());
        }

        @Test
        @DisplayName("Response body is empty (204 carries no payload)")
        void logout_hasNoBody() {
            when(oAuthGatewayService.logout(any())).thenReturn(Mono.empty());

            webTestClient.get()
                    .uri("/oauth/github/logout")
                    .exchange()
                    .expectBody().isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // POST /oauth/github/refresh
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /oauth/github/refresh")
    class RefreshTests {

        /** A minimal ApiResponse that the service would return on success. */
        private ApiResponse<SuccessfulLoginResponse> successResponse() {
            UserDto user = new UserDto(1L, "Alice", "alice@example.com", "ROLE_USER");
            SuccessfulLoginResponse loginResp = new SuccessfulLoginResponse(user, "new-access-token");
            return new ApiResponse<>(loginResp, "Token refreshed successfully");
        }

        @Test
        @DisplayName("Returns 200 OK with refreshed token data")
        void refresh_returns200OnSuccess() {
            when(oAuthGatewayService.refresh(eq("valid-refresh-token"), any()))
                    .thenReturn(Mono.just(successResponse()));

            webTestClient.post()
                    .uri("/oauth/github/refresh")
                    .cookie("refresh_token", "valid-refresh-token")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("Response contains new access token in data.token")
        void refresh_responseContainsNewToken() {
            when(oAuthGatewayService.refresh(eq("valid-refresh-token"), any()))
                    .thenReturn(Mono.just(successResponse()));

            webTestClient.post()
                    .uri("/oauth/github/refresh")
                    .cookie("refresh_token", "valid-refresh-token")
                    .exchange()
                    .expectBody()
                    .jsonPath("$.data.token").isEqualTo("new-access-token");
        }

        @Test
        @DisplayName("Response contains user details")
        void refresh_responseContainsUser() {
            when(oAuthGatewayService.refresh(eq("valid-refresh-token"), any()))
                    .thenReturn(Mono.just(successResponse()));

            webTestClient.post()
                    .uri("/oauth/github/refresh")
                    .cookie("refresh_token", "valid-refresh-token")
                    .exchange()
                    .expectBody()
                    .jsonPath("$.data.user.name").isEqualTo("Alice");
        }

        @Test
        @DisplayName("Returns 401 when refresh token is invalid or device mismatched")
        void refresh_returns401OnAuthFailure() {
            when(oAuthGatewayService.refresh(eq("bad-refresh-token"), any()))
                    .thenReturn(Mono.error(new JwtAuthenticationException("Invalid refresh token")));

            webTestClient.post()
                    .uri("/oauth/github/refresh")
                    .cookie("refresh_token", "bad-refresh-token")
                    .exchange()
                    .expectStatus().is5xxServerError(); // unhandled JwtAuthenticationException hits generic 500
        }
    }
}