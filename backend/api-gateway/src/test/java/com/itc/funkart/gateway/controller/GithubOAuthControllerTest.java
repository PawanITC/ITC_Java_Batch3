package com.itc.funkart.gateway.controller;


import com.itc.funkart.gateway.config.props.ApiProperties;
import com.itc.funkart.gateway.config.props.FrontendProperties;
import com.itc.funkart.gateway.config.props.GitHubProperties;
import com.itc.funkart.gateway.exception.OAuthException;
import com.itc.funkart.gateway.security.CookieUtil;
import com.itc.funkart.gateway.security.JwtTokenValidator;
import com.itc.funkart.gateway.security.JwtWebFilter;
import com.itc.funkart.gateway.service.GithubOAuthService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * <h2>GitHub OAuth Controller Web Layer Tests</h2>
 *
 * <p>
 * This test class verifies the HTTP-layer behavior of {@link GithubOAuthController}
 * in isolation from the rest of the application context.
 * </p>
 */
@WebFluxTest(controllers = GithubOAuthController.class)
@Import(GithubOAuthControllerTest.MockConfig.class)
class GithubOAuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private GithubOAuthService githubOAuthService;

    @MockitoBean
    private CookieUtil cookieUtil;

    @MockitoBean
    private JwtTokenValidator jwtTokenValidator;

    @MockitoBean
    private JwtWebFilter jwtWebFilter;

    @BeforeEach
    void setUp() {
        // Use doAnswer to avoid calling the real filter logic during stubbing
        doAnswer(invocation -> {
            ServerWebExchange exchange = invocation.getArgument(0);
            WebFilterChain chain = invocation.getArgument(1);
            return chain.filter(exchange);
        }).when(jwtWebFilter).filter(any(ServerWebExchange.class), any(WebFilterChain.class));
    }

    /**
     * <h3>Login Endpoint</h3>
     *
     * <p>
     * Validates that the OAuth login endpoint correctly redirects the user
     * to the GitHub authorization URL.
     * </p>
     *
     * <p>
     * This is a pure routing test — no service interaction is required.
     * </p>
     */
    @Test
    @DisplayName("Login: redirects to GitHub")
    void login_redirectsToGitHub() {
        webTestClient.get()
                .uri("/oauth/github/login")
                .exchange()
                .expectStatus().isTemporaryRedirect();
    }

    /**
     * <h3>Logout Endpoint</h3>
     *
     * <p>
     * Ensures logout correctly clears the authentication cookie
     * and returns a success response.
     * </p>
     */
    @Test
    @DisplayName("Logout: clears cookie and returns success response")
    void logout_clearsCookie() {
        webTestClient.get()
                .uri("/oauth/github/logout")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Logged out successfully");

        verify(cookieUtil, times(1)).clearTokenCookie(any());
    }

    // This nested config satisfies WebConfig and the Controller dependencies
    @TestConfiguration
    static class MockConfig {

        @Bean
        @Primary
        public SecurityWebFilterChain springSecurityFilterChain(
                ServerHttpSecurity http) {
            return http
                    .csrf(org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec::disable)
                    .authorizeExchange(ex -> ex.anyExchange().permitAll())
                    .build();
        }

        @Bean
        ApiProperties apiProperties() {
            return new ApiProperties("/api/v1");
        }

        @Bean
        GitHubProperties gitHubProperties() {
            return new GitHubProperties("test-client-id", "test-secret", "http://callback");
        }

        @Bean
        FrontendProperties frontendProperties() {
            return new FrontendProperties("http://localhost:5173");
        }
    }

    /**
     * <h3>Callback Flow</h3>
     *
     * <p>
     * Tests the OAuth callback success path:
     * <ul>
     *     <li>Authorization code is exchanged for JWT</li>
     *     <li>JWT is stored in cookie</li>
     *     <li>User is redirected to frontend</li>
     * </ul>
     * </p>
     */
    @Nested
    class CallbackTests {
        @Test
        @DisplayName("Callback: success sets cookie and redirects")
        void callback_success() {
            when(githubOAuthService.processCode("code"))
                    .thenReturn(Mono.just("jwt-token"));

            webTestClient.get()
                    .uri(uri -> uri.path("/oauth/github/callback")
                            .queryParam("code", "code")
                            .build())
                    .exchange()
                    .expectStatus().isTemporaryRedirect()
                    .expectHeader().location("http://localhost:5173");

            // Adjusted to match your specific CookieUtil signature
            verify(cookieUtil).addTokenCookie(any(), eq("jwt-token"), any());
        }

        @Test
        @DisplayName("Callback: OAuth failure returns error")
        void callback_failure() {
            when(githubOAuthService.processCode("bad"))
                    .thenReturn(Mono.error(new OAuthException("GitHub Exchange Failed")));

            webTestClient.get()
                    .uri(uri -> uri.path("/oauth/github/callback")
                            .queryParam("code", "bad")
                            .build())
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    // Verify this matches your GlobalExceptionHandler's JSON structure
                    .jsonPath("$.error.message").isEqualTo("GitHub Exchange Failed");
        }
    }
}