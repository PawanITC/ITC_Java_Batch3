package com.itc.funkart.user.service;

import com.itc.funkart.user.config.GithubOAuthConfig;
import com.itc.funkart.user.entity.OAuthAccount;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.OAuthException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Technical test suite for {@link GithubOAuthService} integration.
 * <p>Uses {@link MockWebServer} to intercept outbound REST calls and simulate
 * GitHub API responses. This approach ensures 100% coverage on:
 * <ul>
 * <li>JSON DTO mapping for GitHub records.</li>
 * <li>Ternary logic for synthetic email and name fallbacks.</li>
 * <li>Exception translation from WebClient errors to domain-specific {@link OAuthException}.</li>
 * </ul>
 * </p>
 */
class GithubOAuthServiceTest {

    private static MockWebServer mockServer;
    private GithubOAuthService service;
    private AutoCloseable closeable;

    @Mock private GithubOAuthConfig config;
    @Mock private UserService userService;
    @Mock private OAuthAccountService oAuthAccountService;

    /**
     * Initializes the MockWebServer before the test suite begins.
     * @throws IOException If the server fails to start.
     */
    @BeforeAll
    static void start() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    /**
     * Shuts down the MockWebServer after all tests have completed.
     * @throws IOException If the server fails to stop.
     */
    @AfterAll
    static void stop() throws IOException {
        mockServer.shutdown();
    }

    /**
     * Sets up the test harness before each execution.
     * <p>Constructs a fresh {@link WebClient} pointed at the local mock server
     * and injects it into the service instance.</p>
     */
    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        WebClient webClient = WebClient.builder()
                .baseUrl(mockServer.url("/").toString())
                .build();

        service = new GithubOAuthService(config, webClient, oAuthAccountService, userService);

        // Standard configuration mocks used across all handshake scenarios
        when(config.getClientId()).thenReturn("mock-id");
        when(config.getClientSecret()).thenReturn("mock-secret");
        when(config.getRedirectUri()).thenReturn("http://localhost:3000/callback");
    }

    /**
     * Releases Mockito resources after each test.
     * @throws Exception if closing fails.
     */
    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    /**
     * Test group focusing on profile data normalization and fallback mechanisms.
     */
    @Nested
    @DisplayName("Email & Name Fallback Logic")
    class FallbackSuites {

        /**
         * Verifies the generation of a synthetic email and handle-based name
         * when the GitHub profile returns null for public fields.
         * <p>Logic Path: {@code githubUser.login() + "@github.oauth"}</p>
         */
        @Test
        @DisplayName("Should generate synthetic email and fallback name when GitHub returns nulls")
        void github_SyntheticMapping() {
            mockServer.enqueue(new MockResponse()
                    .setBody("{\"access_token\":\"tkn\"}")
                    .addHeader("Content-Type", "application/json"));

            mockServer.enqueue(new MockResponse()
                    .setBody("{\"id\":123, \"login\":\"gituser\", \"email\":null, \"name\":null}")
                    .addHeader("Content-Type", "application/json"));

            when(userService.findByEmail(anyString())).thenReturn(Optional.empty());
            when(userService.createUser(anyString(), isNull(), anyString()))
                    .thenReturn(User.builder().id(1L).build());
            when(oAuthAccountService.findOrCreate(anyLong(), eq("github"), eq("123")))
                    .thenReturn(new OAuthAccount());

            User result = service.processCode("valid_code");

            assertNotNull(result);
            verify(userService).createUser(eq("gituser@github.oauth"), isNull(), eq("gituser"));
        }

        /**
         * Verifies a successful handshake when GitHub returns a full, valid profile.
         */
        @Test
        @DisplayName("Success: Should process valid code and return synchronized user")
        void github_FullSuccess() {
            mockServer.enqueue(new MockResponse().setBody("{\"access_token\":\"tkn\"}")
                    .addHeader("Content-Type", "application/json"));
            mockServer.enqueue(new MockResponse()
                    .setBody("{\"id\":555, \"login\":\"tester\", \"email\":\"test@itc.com\", \"name\":\"Tester\"}")
                    .addHeader("Content-Type", "application/json"));

            when(userService.findByEmail("test@itc.com")).thenReturn(Optional.empty());
            when(userService.createUser(any(), any(), any())).thenReturn(User.builder().id(1L).build());

            User result = service.processCode("valid_code");

            assertNotNull(result);
            verify(oAuthAccountService).findOrCreate(anyLong(), eq("github"), eq("555"));
        }
    }

    /**
     * Test group verifying persistence logic and account linking.
     */
    @Nested
    @DisplayName("Persistence & Account Linking")
    class PersistenceSuites {

        /**
         * Verifies that the service retrieves an existing local user by email
         * instead of creating a duplicate entry during the OAuth flow.
         */
        @Test
        @DisplayName("Should link to existing user if email is already in database")
        void github_ExistingUser() {
            mockServer.enqueue(new MockResponse().setBody("{\"access_token\":\"tkn\"}")
                    .addHeader("Content-Type", "application/json"));
            mockServer.enqueue(new MockResponse()
                    .setBody("{\"id\":123, \"login\":\"gituser\", \"email\":\"real@test.com\"}")
                    .addHeader("Content-Type", "application/json"));

            User existingUser = User.builder().id(10L).email("real@test.com").build();
            when(userService.findByEmail("real@test.com")).thenReturn(Optional.of(existingUser));

            service.processCode("code");

            verify(userService, never()).createUser(any(), any(), any());
            verify(oAuthAccountService).findOrCreate(eq(10L), eq("github"), eq("123"));
        }
    }

    /**
     * Test group ensuring robust error translation for downstream API failures.
     */
    @Nested
    @DisplayName("Exception & Error Handling")
    class ErrorSuites {

        /**
         * Verifies that an HTTP 401 from GitHub results in a domain-specific {@link OAuthException}.
         */
        @Test
        @DisplayName("Should throw OAuthException when GitHub returns 401 Unauthorized")
        void github_Unauthorized() {
            mockServer.enqueue(new MockResponse().setResponseCode(401));

            assertThrows(OAuthException.class, () -> service.processCode("invalid_code"));
        }

        /**
         * Verifies that internal server errors during user profile retrieval
         * are caught and re-thrown as {@link OAuthException}.
         */
        @Test
        @DisplayName("Should throw OAuthException when GitHub user profile fetch fails")
        void github_UserFetchFail() {
            mockServer.enqueue(new MockResponse().setBody("{\"access_token\":\"tkn\"}")
                    .addHeader("Content-Type", "application/json"));
            mockServer.enqueue(new MockResponse().setResponseCode(500));

            assertThrows(OAuthException.class, () -> service.processCode("code"));
        }
    }
}