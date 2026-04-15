package com.itc.funkart.gateway.controller;

import com.itc.funkart.gateway.config.AppConfig;
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
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;


/**
 * Integration tests for {@link GithubOAuthController}.
 * <p>
 * Validates the GitHub OAuth infrastructure routes. These routes are explicitly excluded
 * from the {@code /api/v1} prefix in {@code WebConfig} to remain at the root level.
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GithubOAuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private GithubOAuthService githubOAuthService;

    @MockitoBean
    private CookieUtil cookieUtil;

    /**
     * Deep stubs are required to handle nested calls in Configuration classes (WebConfig, CorsConfig)
     * during the Spring Context bootstrap phase.
     */
    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    private AppConfig appConfig;

    @MockitoBean
    private JwtTokenValidator jwtTokenValidator;

    @MockitoBean
    private JwtWebFilter jwtWebFilter;

    /**
     * Pre-configures the mocks with specific values required for the test cases.
     * Includes a pass-through for the security filter to prevent NPEs during reactive chain execution.
     */
    @BeforeEach
    void setUp() {
        // Clear everything to prevent "TooManyActualInvocations" or "Wrong Arguments"
        reset(githubOAuthService, cookieUtil, appConfig, jwtTokenValidator, jwtWebFilter); // <-- FIX 2

        // RE-APPLY your Security Filter Fix after the reset
        when(jwtWebFilter.filter(any(), any())).thenAnswer(invocation -> {
            org.springframework.web.server.ServerWebExchange exchange = invocation.getArgument(0);
            org.springframework.web.server.WebFilterChain chain = invocation.getArgument(1);
            return chain.filter(exchange);
        });

        // 2. API & Frontend Config
        when(appConfig.api().version()).thenReturn("/api/v1");
        when(appConfig.frontendUrl()).thenReturn("http://localhost:5173");

        // 3. GitHub OAuth Config
        when(appConfig.github().clientId()).thenReturn("test-id");
        when(appConfig.github().clientSecret()).thenReturn("test-secret");
        when(appConfig.github().redirectUri()).thenReturn("http://localhost:8080/oauth/github/callback");

        // 4. Internal Services Config
        when(appConfig.services().userServiceUrl()).thenReturn("http://localhost:8081");

        // 5. JWT Config
        when(appConfig.jwt().cookieName()).thenReturn("token");
        when(appConfig.jwt().secret()).thenReturn("mXxbssTvw7+m8gAlzYhTCP4IyxMeOK2tDqSDPQAv6Qk=");
    }

    /**
     * Confirms /oauth/github/login is NOT prefixed and redirects correctly.
     */
    @Test
    @DisplayName("Login: Should redirect to GitHub at the root path")
    void whenLogin_thenRedirectToGitHub() {
        webTestClient.get()
                .uri("/oauth/github/login")
                .exchange()
                .expectStatus().isTemporaryRedirect()
                .expectHeader().valueMatches("Location", ".*client_id=test-id.*");
    }

    /**
     * Confirms /oauth/github/logout clears session data.
     */
    @Test
    @DisplayName("Logout: Should clear cookie and return success message")
    void whenLogout_thenClearCookie() {
        webTestClient.get()
                .uri("/oauth/github/logout")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Logged out successfully");

        verify(cookieUtil).clearTokenCookie(any());
    }

    /**
     * Tests related to the OAuth2 callback processing and error scenarios.
     */
    @Nested
    @DisplayName("Callback Handling")
    class CallbackTests {

        /**
         * Verifies that the callback processes the authorization code, issues a cookie,
         * and redirects the user to the frontend dashboard.
         */
        @Test
        @DisplayName("Callback: Should set cookie and redirect to frontend")
        void whenCallback_thenProcessCodeAndRedirect() {
            String code = "gh-test-code";
            String mockJwt = "mock-jwt-token";

            when(githubOAuthService.processCode(code)).thenReturn(Mono.just(mockJwt));

            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/oauth/github/callback")
                            .queryParam("code", code)
                            .build())
                    .exchange()
                    .expectStatus().isTemporaryRedirect()
                    .expectHeader().location("http://localhost:5173");

            verify(cookieUtil).addTokenCookie(any(), eq(mockJwt), any());
        }

        /**
         * Verifies that if the service fails, the GlobalExceptionHandler catches the error
         * and returns a properly formatted ApiResponse.
         */
        @Test
        @DisplayName("Callback: Should handle OAuthException via GlobalExceptionHandler")
        void whenCallbackServiceFails_thenResponseIsError() {
            String code = "bad-code";
            when(githubOAuthService.processCode(code))
                    .thenReturn(Mono.error(new OAuthException("GitHub Exchange Failed")));

            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/oauth/github/callback")
                            .queryParam("code", code)
                            .build())
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.error.message").isEqualTo("GitHub Exchange Failed")
                    .jsonPath("$.timestamp").exists();
        }
    }
}