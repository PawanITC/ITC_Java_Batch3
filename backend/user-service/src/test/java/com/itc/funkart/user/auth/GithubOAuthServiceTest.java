package com.itc.funkart.user.auth;

import com.itc.funkart.user.config.GithubOAuthConfig;
import com.itc.funkart.user.dto.user.OAuthUserResult;
import com.itc.funkart.user.entity.OAuthAccount;
import com.itc.funkart.user.entity.Role;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.OAuthException;
import com.itc.funkart.user.service.KafkaEventPublisher;
import com.itc.funkart.user.service.OAuthAccountService;
import com.itc.funkart.user.service.UserService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>GithubOAuthService — Unit Tests</h2>
 *
 * <p>Uses {@link MockWebServer} to intercept outbound HTTP calls so the full
 * OAuth exchange pipeline is exercised without hitting the real GitHub API.
 *
 * <p><b>Flow under test:</b>
 * <ol>
 *   <li>POST to GitHub token endpoint → returns access token</li>
 *   <li>GET GitHub user profile → returns {@code GithubUser} JSON</li>
 *   <li>Resolve or create local user via {@link UserService#getOrCreateOAuthUser}</li>
 *   <li>Ensure OAuth account is linked via {@link OAuthAccountService#findOrCreate}</li>
 *   <li>Publish signup event via {@link KafkaEventPublisher#publishSignup} only for new users</li>
 * </ol>
 *
 * <p><b>Key constructor contract:</b> {@link GithubOAuthService} takes
 * {@code (GithubOAuthConfig, WebClient, OAuthAccountService, UserService, KafkaEventPublisher)}.
 * The old tests were missing {@link KafkaEventPublisher} — that caused silent NPEs.
 */
@ExtendWith(MockitoExtension.class)
class GithubOAuthServiceTest {

    private static MockWebServer mockServer;

    @Mock
    private GithubOAuthConfig config;
    @Mock
    private OAuthAccountService oAuthAccountService;
    @Mock
    private UserService userService;
    @Mock
    private KafkaEventPublisher kafkaEventPublisher;

    private GithubOAuthService service;

    @BeforeAll
    static void startServer() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @AfterAll
    static void stopServer() throws IOException {
        mockServer.shutdown();
    }

    @BeforeEach
    void setUp() {
        WebClient webClient = WebClient.builder()
                .baseUrl(mockServer.url("/").toString())
                .build();

        service = new GithubOAuthService(
                config, webClient, oAuthAccountService, userService, kafkaEventPublisher
        );
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User buildUser(Long id, String email, String name) {
        return User.builder()
                .id(id)
                .email(email)
                .name(name)
                .role(Role.ROLE_USER)
                .build();
    }

    private void enqueueTokenResponse() {
        mockServer.enqueue(new MockResponse()
                .setBody("{\"access_token\":\"gh-access-token\",\"token_type\":\"bearer\"}")
                .addHeader("Content-Type", "application/json"));
    }

    private void enqueueUserProfile(String json) {
        mockServer.enqueue(new MockResponse()
                .setBody(json)
                .addHeader("Content-Type", "application/json"));
    }

    // -------------------------------------------------------------------------
    // Happy path — new user
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("New user flow")
    class NewUserFlowTests {

        @Test
        @DisplayName("Returns the resolved User entity on full success")
        void returnsUser() {
            enqueueTokenResponse();
            enqueueUserProfile(
                    "{\"id\":555,\"login\":\"alice\",\"email\":\"alice@example.com\",\"name\":\"Alice\"}");

            User newUser = buildUser(1L, "alice@example.com", "Alice");
            when(userService.getOrCreateOAuthUser("alice@example.com", "alice"))
                    .thenReturn(new OAuthUserResult(newUser, true));
            when(oAuthAccountService.findOrCreate(eq(newUser), eq("github"), eq("555")))
                    .thenReturn(new OAuthAccount());

            User result = service.processCode("auth-code");

            assertNotNull(result);
            assertEquals("alice@example.com", result.getEmail());
        }

        @Test
        @DisplayName("Publishes signup event only when user is newly created")
        void publishesSignupForNewUser() {
            enqueueTokenResponse();
            enqueueUserProfile(
                    "{\"id\":555,\"login\":\"alice\",\"email\":\"alice@example.com\",\"name\":\"Alice\"}");

            User newUser = buildUser(1L, "alice@example.com", "Alice");
            when(userService.getOrCreateOAuthUser(anyString(), anyString()))
                    .thenReturn(new OAuthUserResult(newUser, true));
            when(oAuthAccountService.findOrCreate(any(), anyString(), anyString()))
                    .thenReturn(new OAuthAccount());

            service.processCode("auth-code");

            verify(kafkaEventPublisher).publishSignup(newUser);
        }

        @Test
        @DisplayName("Links OAuth account via OAuthAccountService.findOrCreate")
        void linksOAuthAccount() {
            enqueueTokenResponse();
            enqueueUserProfile(
                    "{\"id\":555,\"login\":\"alice\",\"email\":\"alice@example.com\",\"name\":\"Alice\"}");

            User newUser = buildUser(1L, "alice@example.com", "Alice");
            when(userService.getOrCreateOAuthUser(anyString(), anyString()))
                    .thenReturn(new OAuthUserResult(newUser, true));
            when(oAuthAccountService.findOrCreate(eq(newUser), eq("github"), eq("555")))
                    .thenReturn(new OAuthAccount());

            service.processCode("auth-code");

            verify(oAuthAccountService).findOrCreate(newUser, "github", "555");
        }
    }

    // -------------------------------------------------------------------------
    // Happy path — existing user
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Existing user flow")
    class ExistingUserFlowTests {

        @Test
        @DisplayName("Does NOT publish signup event for an existing user")
        void doesNotPublishSignupForExistingUser() {
            enqueueTokenResponse();
            enqueueUserProfile(
                    "{\"id\":123,\"login\":\"alice\",\"email\":\"alice@example.com\",\"name\":\"Alice\"}");

            User existingUser = buildUser(10L, "alice@example.com", "Alice");
            when(userService.getOrCreateOAuthUser("alice@example.com", "alice"))
                    .thenReturn(new OAuthUserResult(existingUser, false));
            when(oAuthAccountService.findOrCreate(eq(existingUser), eq("github"), eq("123")))
                    .thenReturn(new OAuthAccount());

            service.processCode("auth-code");

            verify(kafkaEventPublisher, never()).publishSignup(any());
        }

        @Test
        @DisplayName("Still links OAuth account for an existing user")
        void stillLinksOAuthAccountForExistingUser() {
            enqueueTokenResponse();
            enqueueUserProfile(
                    "{\"id\":123,\"login\":\"alice\",\"email\":\"alice@example.com\"}");

            User existingUser = buildUser(10L, "alice@example.com", "Alice");
            when(userService.getOrCreateOAuthUser(anyString(), anyString()))
                    .thenReturn(new OAuthUserResult(existingUser, false));
            when(oAuthAccountService.findOrCreate(eq(existingUser), eq("github"), eq("123")))
                    .thenReturn(new OAuthAccount());

            service.processCode("auth-code");

            verify(oAuthAccountService).findOrCreate(existingUser, "github", "123");
        }
    }

    // -------------------------------------------------------------------------
    // Email / name fallback normalisation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Email and name normalisation")
    class NormalisationTests {

        @Test
        @DisplayName("Generates synthetic email (login@github.oauth) when GitHub returns null email")
        void syntheticEmailWhenNull() {
            enqueueTokenResponse();
            enqueueUserProfile(
                    "{\"id\":999,\"login\":\"gituser\",\"email\":null,\"name\":null}");

            User user = buildUser(1L, "gituser@github.oauth", "gituser");
            when(userService.getOrCreateOAuthUser("gituser@github.oauth", "gituser"))
                    .thenReturn(new OAuthUserResult(user, true));
            when(oAuthAccountService.findOrCreate(any(), anyString(), anyString()))
                    .thenReturn(new OAuthAccount());

            service.processCode("auth-code");

            // Email passed to getOrCreateOAuthUser must be the synthetic address
            verify(userService).getOrCreateOAuthUser("gituser@github.oauth", "gituser");
        }

        @Test
        @DisplayName("Uses GitHub login as name when GitHub returns blank name")
        void usesLoginAsNameWhenNameBlank() {
            enqueueTokenResponse();
            enqueueUserProfile(
                    "{\"id\":999,\"login\":\"gituser\",\"email\":\"real@example.com\",\"name\":\"\"}");

            User user = buildUser(1L, "real@example.com", "gituser");
            when(userService.getOrCreateOAuthUser("real@example.com", "gituser"))
                    .thenReturn(new OAuthUserResult(user, true));
            when(oAuthAccountService.findOrCreate(any(), anyString(), anyString()))
                    .thenReturn(new OAuthAccount());

            service.processCode("auth-code");

            verify(userService).getOrCreateOAuthUser("real@example.com", "gituser");
        }
    }

    // -------------------------------------------------------------------------
    // Error handling
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Throws OAuthException when GitHub token endpoint returns 401")
        void throwsOnUnauthorizedTokenResponse() {

            when(config.getTokenUrl())
                    .thenReturn(mockServer.url("/login/oauth/access_token").toString());

            when(config.getClientId()).thenReturn("mock-client-id");
            when(config.getClientSecret()).thenReturn("mock-client-secret");
            when(config.getRedirectUri()).thenReturn("http://localhost");

            mockServer.enqueue(new MockResponse()
                    .setResponseCode(401)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{}")); // Explicit body prevents stream-read hangs

            assertThrows(OAuthException.class, () -> service.processCode("bad-code"));
        }

        @Test
        @DisplayName("Throws OAuthException when GitHub user profile endpoint returns 500")
        void throwsOnUserProfileServerError() {
            enqueueTokenResponse();
            mockServer.enqueue(new MockResponse().setResponseCode(500));

            assertThrows(OAuthException.class, () -> service.processCode("auth-code"));
        }

        @Test
        @DisplayName("Throws OAuthException when access token in response is null")
        void throwsWhenAccessTokenNull() {

            when(config.getTokenUrl())
                    .thenReturn(mockServer.url("/login/oauth/access_token").toString());

            when(config.getClientId()).thenReturn("mock-client-id");
            when(config.getClientSecret()).thenReturn("mock-client-secret");
            when(config.getRedirectUri()).thenReturn("http://localhost");

            mockServer.enqueue(new MockResponse()
                    .setBody("{\"error\":\"bad_verification_code\"}")
                    .addHeader("Content-Type", "application/json"));

            assertThrows(OAuthException.class, () -> service.processCode("invalid-code"));
        }
    }
}