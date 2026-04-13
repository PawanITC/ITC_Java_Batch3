package com.itc.funkart.user.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.user.config.ApiConfig;
import com.itc.funkart.user.dto.user.JwtUserDto;
import com.itc.funkart.user.dto.user.LoginRequest;
import com.itc.funkart.user.dto.user.SignupRequest;
import com.itc.funkart.user.dto.user.SuccessfulLoginResponse;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.mapper.UserMapper;
import com.itc.funkart.user.service.GithubOAuthService;
import com.itc.funkart.user.service.JwtService;
import com.itc.funkart.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security Integration Test Suite for Funkart User Service.
 * <p>
 * This suite validates the security perimeter, ensuring that public endpoints are accessible
 * and protected resources require a valid {@link JwtUserDto} principal.
 * By using {@link SpringBootTest}, we ensure the API versioning and filter chain logic
 * are tested in a production-near environment.
 * </p>
 *
 * @version 3.2
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ApiConfig apiConfig;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;
    @MockBean private UserMapper userMapper;
    @MockBean private JwtService jwtService;
    @MockBean private GithubOAuthService githubOAuthService;

    /**
     * Helper to construct the versioned API URL.
     */
    private String getUrl(String path) {
        // Make sure this matches the prefix your Gateway uses!
        String version = apiConfig.getVersion();
        return "/api/" + version + "/users" + path;
    }

    /**
     * Helper to create a standard successful response mock.
     */
    private SuccessfulLoginResponse createMockResponse() {
        return new SuccessfulLoginResponse(101L, "Tester", "tester@example.com", "fake-jwt-token");
    }

    /**
     * Serializes a Java object into a JSON string for request bodies.
     */
    private String asJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    @Nested
    @DisplayName("Public Access Tests")
    class PublicEndpoints {

        @Test
        @DisplayName("POST /signup - Should be publicly accessible")
        void signup_IsPublic() throws Exception {
            when(userService.signUp(any())).thenReturn(new User());
            when(jwtService.generateJwtToken(any())).thenReturn("token");
            when(userMapper.toResponse(any(), any())).thenReturn(createMockResponse());

            SignupRequest request = new SignupRequest("tester@example.com", "password123", "Tester");

            mockMvc.perform(post(getUrl("/signup"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJson(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("POST /login - Should be publicly accessible")
        void login_IsPublic() throws Exception {
            when(userService.login(any())).thenReturn(new User());
            when(jwtService.generateJwtToken(any())).thenReturn("token");
            when(userMapper.toResponse(any(), any())).thenReturn(createMockResponse());

            LoginRequest request = new LoginRequest("tester@example.com", "password123");

            mockMvc.perform(post(getUrl("/login"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJson(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /oauth/github - Should be publicly accessible")
        void oauth_IsPublic() throws Exception {
            when(githubOAuthService.processCode(any())).thenReturn(new User());
            when(jwtService.generateJwtToken(any())).thenReturn("token");
            when(userMapper.toResponse(any(), any())).thenReturn(createMockResponse());

            Map<String, String> request = Map.of("code", "github_test_code");

            mockMvc.perform(post(getUrl("/oauth/github"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJson(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Protected Access Tests")
    class ProtectedEndpoints {

        @Test
        @DisplayName("GET /me - Should return 403 when no user is present")
        void getMe_Anonymous_IsForbidden() throws Exception {
            mockMvc.perform(get(getUrl("/me")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /me - Should return 200 when authenticated with custom principal")
        void getMe_Authenticated_IsAllowed() throws Exception {
            JwtUserDto principal = new JwtUserDto(101L, "Tester", "tester@example.com");
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());

            when(userService.findById(any())).thenReturn(Optional.of(new User()));
            when(userMapper.toResponse(any(), any())).thenReturn(createMockResponse());

            mockMvc.perform(get(getUrl("/me"))
                            .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Security Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Verify security filter takes precedence over method matching")
        void protectedRoute_wrongMethod_IsForbiddenFirst() throws Exception {
            mockMvc.perform(post(getUrl("/me")))
                    .andExpect(status().isForbidden());
        }
    }
}