package com.itc.funkart.gateway.controller;

import com.itc.funkart.gateway.config.WebConfig;
import com.itc.funkart.gateway.config.props.ApiProperties;
import com.itc.funkart.gateway.config.props.FrontendProperties;
import com.itc.funkart.gateway.dto.request.LoginRequest;
import com.itc.funkart.gateway.security.CookieUtil;
import com.itc.funkart.gateway.security.JwtTokenValidator;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>Robust User Controller Tests</h2>
 * Uses MockWebServer to simulate the User-Service, avoiding fragile WebClient mocking.
 */
@WebFluxTest(controllers = UserController.class)
@Import({UserControllerTest.SecurityMockConfig.class, WebConfig.class})
class UserControllerTest {

    @Autowired private WebTestClient webTestClient;
    @MockitoBean private CookieUtil cookieUtil;
    @MockitoBean private JwtTokenValidator jwtTokenValidator;

    private static MockWebServer mockBackend;

    @BeforeAll
    static void setUpServer() throws IOException {
        mockBackend = new MockWebServer();
        mockBackend.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackend.shutdown();
    }

    @BeforeEach
    void setUp() {
        // Still need to mock the reactive cookie method
        when(cookieUtil.addTokenCookie(any(), anyString())).thenReturn(Mono.empty());
    }

    @TestConfiguration
    static class SecurityMockConfig {
        @Bean
        @Primary
        public WebClient userWebClient() {
            // Point the WebClient at our local MockWebServer instead of a real microservice
            return WebClient.builder()
                    .baseUrl(mockBackend.url("/").toString())
                    .build();
        }

        @Bean public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
            return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .authorizeExchange(ex -> ex.anyExchange().permitAll()).build();
        }
        @Bean public ApiProperties apiProperties() { return new ApiProperties("/api/v1"); }
        @Bean public FrontendProperties frontendProperties() { return new FrontendProperties("http://localhost:5173"); }
    }

    @Test
    @DisplayName("Login: Success via MockWebServer")
    void login_success() {
        // 1. Define what the "User-Service" should return
        String jsonResponse = """
            {
                "data": {
                    "user": { "id": 1, "name": "Test User", "email": "test@test.com", "role": "ROLE_USER" },
                    "token": "real-fake-jwt-token"
                },
                "message": "Login Successful"
            }
            """;

        mockBackend.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // 2. Perform the test
        webTestClient.post()
                .uri("/api/v1/users/login")
                .bodyValue(new LoginRequest("test@test.com", "password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.user.name").isEqualTo("Test User");

        // 3. Verify side effects
        verify(cookieUtil).addTokenCookie(any(), eq("real-fake-jwt-token"));
    }
}