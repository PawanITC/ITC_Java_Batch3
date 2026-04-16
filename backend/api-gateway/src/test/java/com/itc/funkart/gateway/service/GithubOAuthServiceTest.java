package com.itc.funkart.gateway.service;

import com.itc.funkart.gateway.config.props.ApiProperties;
import com.itc.funkart.gateway.exception.OAuthException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GithubOAuthService}.
 *
 * <p>
 * Validates OAuth code exchange through a mocked downstream User-Service
 * using a deterministic HTTP simulation layer.
 * </p>
 */
class GithubOAuthServiceTest {

    private static MockWebServer mockBackend;

    private GithubOAuthService githubOAuthService;

    @BeforeAll
    static void startServer() throws IOException {
        mockBackend = new MockWebServer();
        mockBackend.start();
    }

    @AfterAll
    static void stopServer() throws IOException {
        mockBackend.shutdown();
    }

    @BeforeEach
    void setUp() {

        String baseUrl = mockBackend.url("/").toString();

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();

        // ✅ Mock ApiProperties properly
        ApiProperties apiProperties = mock(ApiProperties.class);
        when(apiProperties.version()).thenReturn("/api/v1");

        githubOAuthService = new GithubOAuthService(webClient, apiProperties);
    }

    /**
     * Verifies successful OAuth exchange returns JWT token.
     */
    @Test
    void whenProcessCode_thenReturnToken() {

        String response = """
        {
            "data": { "token": "fake-jwt-token" },
            "message": "ok"
        }
        """;

        mockBackend.enqueue(new MockResponse()
                .setBody(response)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(githubOAuthService.processCode("code-123"))
                .expectNext("fake-jwt-token")
                .verifyComplete();
    }

    /**
     * Verifies downstream error is mapped to OAuthException.
     */
    @Test
    void whenUserServiceFails_thenThrowOAuthException() {

        mockBackend.enqueue(new MockResponse()
                .setResponseCode(500));

        StepVerifier.create(githubOAuthService.processCode("bad-code"))
                .expectError(OAuthException.class)
                .verify();
    }
}