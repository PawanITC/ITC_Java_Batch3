package com.itc.funkart.gateway.controller;

import com.itc.funkart.gateway.config.AppConfig;
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
import org.mockito.Answers;
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

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    private AppConfig appConfig;


    /**
     * <h3>Mock Configuration</h3>
     * Satisfies WebConfig and Controller dependencies for the test slice.
     */
    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
            return http
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .authorizeExchange(ex -> ex.anyExchange().permitAll())
                    .build();
        }

        // Ensure these match the property records exactly
        @Bean ApiProperties apiProperties() { return new ApiProperties("/api/v1"); }
        @Bean GitHubProperties gitHubProperties() {
            return new GitHubProperties("test-id", "test-secret", "http://callback");
        }
        @Bean FrontendProperties frontendProperties() {
            return new FrontendProperties("http://localhost:5173");
        }
    }

    @BeforeEach
    void setUp() {
        // 1. BULLETPROOF FILTER STUB
        // We use lenient() to prevent Mockito from complaining about unused stubs
        // and add a null check for the chain to stop the NPE.
        lenient().when(jwtWebFilter.filter(any(), any())).thenAnswer(invocation -> {
            ServerWebExchange exchange = invocation.getArgument(0);
            WebFilterChain chain = invocation.getArgument(1);

            // If the chain is null (happens in some error dispatches),
            // just return Mono.empty() to stop the crash.
            return (chain != null) ? chain.filter(exchange) : Mono.empty();
        });

        // 2. STUB DEEP APPCONFIG
        when(appConfig.jwt().cookieName()).thenReturn("token");
        when(appConfig.github().clientId()).thenReturn("test-client-id");
        when(appConfig.github().redirectUri()).thenReturn("http://localhost:8080/callback");
        when(appConfig.frontendUrl()).thenReturn("http://localhost:5173");

        // 3. STUB COOKIEUTIL
        when(cookieUtil.addTokenCookie(any(), anyString())).thenReturn(Mono.empty());
        when(cookieUtil.clearTokenCookie(any())).thenReturn(Mono.empty());
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

    /**
     * <h3>Callback Flow</h3>
     *
     * <p>
     * Tests the OAuth callback success path:
     * <ul>
     * <li>Authorization code is exchanged for JWT</li>
     * <li>JWT is stored in cookie</li>
     * <li>User is redirected to frontend</li>
     * </ul>
     * </p>
     */
    @Nested
    class CallbackTests {
        @Test
        @DisplayName("Callback: success sets cookie and redirects to frontend")
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

            verify(cookieUtil).addTokenCookie(any(), eq("jwt-token"));
        }

        @Test
        @DisplayName("Callback: OAuth failure returns 401 with error details")
        void callback_failure() {
            when(githubOAuthService.processCode("bad"))
                    .thenReturn(Mono.error(new OAuthException("GitHub Exchange Failed")));

            webTestClient.get()
                    .uri(uri -> uri.path("/oauth/github/callback")
                            .queryParam("code", "bad")
                            .build())
                    .exchange()
                    .expectStatus().isUnauthorized() // Changed from isBadRequest()
                    .expectBody()
                    .jsonPath("$.error.message").isEqualTo("GitHub Exchange Failed");
        }
    }
}