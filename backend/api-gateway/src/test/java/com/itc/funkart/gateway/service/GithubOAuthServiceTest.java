package com.itc.funkart.gateway.service;

import com.itc.funkart.gateway.config.AppConfig;
import com.itc.funkart.gateway.exception.OAuthException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GithubOAuthService} using a mock web server.
 * <p>
 * This class tests the Gateway's ability to act as an orchestration bridge
 * between the frontend and the internal User-Service during the GitHub
 * OAuth handshake. It verifies correct data mapping and error handling
 * within a non-blocking reactive pipeline.
 * </p>
 */
public class GithubOAuthServiceTest {
    /**
     * The mock server used to simulate the User-Service downstream.
     */
    private static MockWebServer mockBackend;
    /**
     * The service under test, injected with a {@link WebClient}
     * pointing to the mock backend.
     */
    private GithubOAuthService githubOAuthService;

    /**
     * Starts the mock server on an ephemeral port before all tests.
     * @throws IOException if the server fails to start.
     */
    @BeforeAll
    static void setUp() throws IOException {
        mockBackend = new MockWebServer();
        mockBackend.start();
    }

    /**
     * Shuts down the mock server after all tests to release resources.
     * @throws IOException if the server fails to stop.
     */
    @AfterAll
    static void tearDown() throws IOException {
        mockBackend.shutdown();
    }

    /**
     * Initializes the service and WebClient before each test,
     * ensuring the client points to the current mock server port.
     */
    @BeforeEach
    void initialize() {
        String baseUrl = String.format("http://localhost:%s", mockBackend.getPort());
        WebClient webClient = WebClient.create(baseUrl);

        // You MUST mock AppConfig here too
        AppConfig appConfig = mock(AppConfig.class, Answers.RETURNS_DEEP_STUBS);
        when(appConfig.api().version()).thenReturn("/api/v1");

        githubOAuthService = new GithubOAuthService(webClient, appConfig);
    }

    /**
     * Verifies the successful exchange of a GitHub code for a JWT.
     * <p>
     * Logic:
     * 1. Enqueues a simulated valid JSON response.
     * 2. Uses {@link StepVerifier} to subscribe to the Mono and assert
     * that the token is correctly extracted.
     * </p>
     */
    @Test
    void whenProcessCode_thenReturnToken() {
        // FIX: Match the new nested ApiResponse structure
        String nestedResponse = """
        {
            "data": { "token": "fake-jwt-token" },
            "message": "Success"
        }
        """;

        mockBackend.enqueue(new MockResponse()
                .setBody(nestedResponse)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(githubOAuthService.processCode("test-github-code"))
                .expectNext("fake-jwt-token")
                .verifyComplete();
    }

    /**
     * Verifies that downstream HTTP errors are gracefully handled.
     * <p>
     * Logic:
     * 1. Enqueues a 500 Internal Server Error.
     * 2. Asserts that the reactive stream emits an {@link OAuthException}
     * instead of a raw WebClient exception.
     * </p>
     */
    @Test
    void whenUserServiceError_thenThrowOAuthException() {
        // 1. Simulate a 500 error from User-Service
        mockBackend.enqueue(new MockResponse().setResponseCode(500));

        // 2. Verify that our service wraps it in an OAuthException
        StepVerifier.create(githubOAuthService.processCode("bad-code"))
                .expectError(OAuthException.class)
                .verify(java.time.Duration.ofSeconds(5));
    }
}
