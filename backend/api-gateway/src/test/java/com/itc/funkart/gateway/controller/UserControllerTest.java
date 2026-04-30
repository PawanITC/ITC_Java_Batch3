package com.itc.funkart.gateway.controller;

import com.itc.funkart.gateway.dto.UserDto;
import com.itc.funkart.gateway.dto.request.LoginRequest;
import com.itc.funkart.gateway.dto.request.SignupRequest;
import com.itc.funkart.gateway.dto.response.SuccessfulLoginResponse;
import com.itc.funkart.gateway.exception.OAuthException;
import com.itc.funkart.gateway.response.ApiResponse;
import com.itc.funkart.gateway.security.JwtAuthWebFilter;
import com.itc.funkart.gateway.service.UserGatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <h2>UserController — Web Layer Tests</h2>
 *
 * <p>Verifies HTTP routing and response shaping for {@link UserController}.
 * The controller is intentionally thin — it delegates everything to
 * {@link UserGatewayService}, which is mocked here.
 *
 * <p><b>Endpoints under test:</b>
 * <ul>
 *   <li>{@code POST /users/login}  — credential-based login</li>
 *   <li>{@code POST /users/signup} — new account creation</li>
 * </ul>
 *
 * <p>Cookie handling is the responsibility of the service layer and is
 * verified through service interaction assertions, not response header inspection.
 */
@WebFluxTest(controllers = UserController.class)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    /**
     * Service is mocked so we control what the "downstream" returns.
     */
    @MockitoBean
    private UserGatewayService userGatewayService;

    /**
     * Required by the security auto-config; passes all requests through transparently.
     */
    @MockitoBean
    private JwtAuthWebFilter jwtAuthWebFilter;

    @BeforeEach
    void setUp() {
        // Robust stubbing to prevent the NPE occuring
        lenient().when(jwtAuthWebFilter.filter(any(), any())).thenAnswer(inv -> {
            ServerWebExchange exchange = inv.getArgument(0);
            WebFilterChain chain = inv.getArgument(1);
            return (chain != null) ? chain.filter(exchange) : Mono.empty();
        });
    }


    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    private ApiResponse<SuccessfulLoginResponse> successfulResponse(String name, String email, String token) {
        UserDto user = new UserDto(1L, name, email, "ROLE_USER");
        SuccessfulLoginResponse body = new SuccessfulLoginResponse(user, token);
        return new ApiResponse<>(body, "Success");
    }

    /**
     * Minimal Security Config for this test slice.
     * Prevents the real SecurityConfig from loading and demanding real dependencies.
     */
    @TestConfiguration
    static class TestSecurity {
        @Bean
        public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
            return http
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .authorizeExchange(ex -> ex.anyExchange().permitAll())
                    .build();
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/users/login
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/users/login")
    class LoginTests {

        @Test
        @DisplayName("Returns 200 OK with user data on successful login")
        void login_returns200() {
            when(userGatewayService.login(any(), any()))
                    .thenReturn(Mono.just(successfulResponse("Alice", "alice@example.com", "jwt")));

            webTestClient.post()
                    .uri("/api/v1/users/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new LoginRequest("alice@example.com", "password123"))
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("Response body contains user name and token")
        void login_verifiesResponseBody() {
            when(userGatewayService.login(any(), any()))
                    .thenReturn(Mono.just(successfulResponse("Alice", "alice@example.com", "my-jwt-token")));

            webTestClient.post()
                    .uri("/api/v1/users/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new LoginRequest("alice@example.com", "password123"))
                    .exchange()
                    .expectBody()
                    .jsonPath("$.data.user.name").isEqualTo("Alice")
                    .jsonPath("$.data.token").isEqualTo("my-jwt-token");
        }

        @Test
        @DisplayName("Delegates to UserGatewayService.login")
        void login_delegatesToService() {
            when(userGatewayService.login(any(), any()))
                    .thenReturn(Mono.just(successfulResponse("Alice", "alice@example.com", "jwt")));

            webTestClient.post()
                    .uri("/api/v1/users/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new LoginRequest("alice@example.com", "pass"))
                    .exchange();

            verify(userGatewayService).login(any(LoginRequest.class), any());
        }

        @Test
        @DisplayName("Returns 401 when downstream raises OAuthException")
        void login_returns401OnAuthFailure() {
            when(userGatewayService.login(any(), any()))
                    .thenReturn(Mono.error(new OAuthException("Invalid credentials")));

            webTestClient.post()
                    .uri("/api/v1/users/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new LoginRequest("bad@example.com", "wrong"))
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectBody()
                    .jsonPath("$.error.code").isEqualTo("OAUTH_ERROR");
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/users/signup
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/users/signup")
    class SignupTests {

        @Test
        @DisplayName("Returns 200 OK with user data on successful signup")
        void signup_returns200() {
            when(userGatewayService.signup(any(), any()))
                    .thenReturn(Mono.just(successfulResponse("Bob", "bob@example.com", "jwt")));

            webTestClient.post()
                    .uri("/api/v1/users/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new SignupRequest("Bob", "bob@example.com", "password123"))
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("Response body contains newly registered user details")
        void signup_verifiesResponseBody() {
            when(userGatewayService.signup(any(), any()))
                    .thenReturn(Mono.just(successfulResponse("Bob", "bob@example.com", "signup-jwt")));

            webTestClient.post()
                    .uri("/api/v1/users/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new SignupRequest("Bob", "bob@example.com", "password123"))
                    .exchange()
                    .expectBody()
                    .jsonPath("$.data.user.name").isEqualTo("Bob")
                    .jsonPath("$.data.token").isEqualTo("signup-jwt");
        }

        @Test
        @DisplayName("Delegates to UserGatewayService.signup")
        void signup_delegatesToService() {
            when(userGatewayService.signup(any(), any()))
                    .thenReturn(Mono.just(successfulResponse("Bob", "bob@example.com", "jwt")));

            webTestClient.post()
                    .uri("/api/v1/users/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new SignupRequest("Bob", "bob@example.com", "password123"))
                    .exchange();

            verify(userGatewayService).signup(any(SignupRequest.class), any());
        }
    }
}