package com.itc.funkart.gateway.controller;

import com.itc.funkart.gateway.config.WebConfig;
import com.itc.funkart.gateway.config.props.ApiProperties;
import com.itc.funkart.gateway.config.props.FrontendProperties;
import com.itc.funkart.gateway.dto.UserDto;
import com.itc.funkart.gateway.dto.request.LoginRequest;
import com.itc.funkart.gateway.dto.request.SignupRequest;
import com.itc.funkart.gateway.dto.response.SuccessfulLoginResponse;
import com.itc.funkart.gateway.response.ApiResponse;
import com.itc.funkart.gateway.security.CookieUtil;
import com.itc.funkart.gateway.security.JwtTokenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@WebFluxTest(controllers = UserController.class)
@Import({UserControllerTest.SecurityMockConfig.class, WebConfig.class})
class UserControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean private CookieUtil cookieUtil;
    @MockitoBean private JwtTokenValidator jwtTokenValidator;
    @MockitoBean private WebClient webClient;

    // Chain Variables
    private WebClient.RequestBodyUriSpec bodyUriSpec;
    private WebClient.RequestBodySpec bodySpec;
    private WebClient.RequestHeadersSpec headersSpec;
    private WebClient.ResponseSpec responseSpec;

    @TestConfiguration
    static class SecurityMockConfig {
        @Bean
        public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
            return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .authorizeExchange(ex -> ex.anyExchange().permitAll())
                    .build();
        }

        @Bean public ApiProperties apiProperties() { return new ApiProperties("v1"); }
        @Bean public FrontendProperties frontendProperties() { return new FrontendProperties("http://localhost:5173"); }
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUpMocks() {
        bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        bodySpec = mock(WebClient.RequestBodySpec.class);
        headersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);

        // Chain setup
        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Login: Success triggers cookie")
    void login_success() {
        var token = "fake-jwt-token";
        var user = new UserDto(1L, "Test", "test@test.com", "ROLE_USER");
        var loginData = new SuccessfulLoginResponse(user, token);
        var wrapped = new ApiResponse<>(loginData, "Login Successful");

        // Use a cast inside the when to tell the compiler we know what we're doing
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(wrapped));

        webTestClient.post()
                .uri("/v1/users/login")
                .bodyValue(new LoginRequest("test@test.com", "password"))
                .exchange()
                .expectStatus().isOk();

        verify(cookieUtil).addTokenCookie(any(), eq(token), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Signup: Success path")
    void signup_success() {
        var token = "signup-token";
        var user = new UserDto(2L, "New User", "new@test.com", "ROLE_USER");
        var signupData = new SuccessfulLoginResponse(user, token);
        var wrapped = new ApiResponse<>(signupData, "User Registered");

        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(wrapped));

        webTestClient.post()
                .uri("/v1/users/signup")
                .bodyValue(new SignupRequest("New User", "new@test.com", "password"))
                .exchange()
                .expectStatus().isOk();

        verify(cookieUtil).addTokenCookie(any(), eq(token), any());
    }
}