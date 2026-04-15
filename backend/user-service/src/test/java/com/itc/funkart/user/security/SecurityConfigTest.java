package com.itc.funkart.user.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.user.config.ApiConfig;
import com.itc.funkart.user.dto.user.*;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.mapper.UserMapper;
import com.itc.funkart.user.service.GithubOAuthService;
import com.itc.funkart.user.service.JwtService;
import com.itc.funkart.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.reset;

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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ApiConfig apiConfig;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private UserMapper userMapper;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private GithubOAuthService githubOAuthService;
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;
    @MockitoBean
    private ProducerFactory<String, Object> producerFactory;

    @BeforeEach
    void setUp() {
        // 2. Clear all mocks manually to be 100% safe
        reset(userService, userMapper, jwtService, githubOAuthService, kafkaTemplate);

        // Your existing Kafka fix
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
    }

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
        // FIX: Use the nested UserDto structure
        UserDto userDto = new UserDto(101L, "tester@example.com", "Tester", "ROLE_USER");
        return new SuccessfulLoginResponse(userDto, "fake-jwt-token");
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

    @BeforeEach
    void setUpKafkaMock() {
        // This ensures that whenever kafkaTemplate.send is called,
        // it returns a completed future instead of null.
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
    }


    @Nested
    @DisplayName("Public Access Tests")
    class PublicEndpoints {

        private User createMockUser() {
            User user = new User();
            user.setId(101L);
            user.setEmail("tester@example.com");
            user.setName("Tester");
            return user;
        }

        @Test
        @DisplayName("POST /signup - Should be publicly accessible")
        void signup_IsPublic() throws Exception {
            when(userService.signUp(any())).thenReturn(createMockUser());
            when(jwtService.generateJwtToken(any())).thenReturn("token");
            when(userMapper.toResponse(any(), any())).thenReturn(createMockResponse());

            SignupRequest request = new SignupRequest("Tester","tester@example.com", "password123");

            mockMvc.perform(post(getUrl("/signup"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJson(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("POST /login - Should be publicly accessible")
        void login_IsPublic() throws Exception {
            when(userService.login(any())).thenReturn(createMockUser());
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
            when(githubOAuthService.processCode(any())).thenReturn(createMockUser());
            when(jwtService.generateJwtToken(any())).thenReturn("token");

            // FIX: Match the Controller's use of toOAuthResponse if applicable,
            // or just ensure the return type matches SuccessfulLoginResponse
            when(userMapper.toResponse(any(), any())).thenReturn(createMockResponse());

            // FIX: Use the new OAuthRequest record structure for the body
            OAuthRequest request = new OAuthRequest("github_test_code");

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
            // FIX: Added the 4th argument (Role) to JwtUserDto
            JwtUserDto principal = new JwtUserDto(101L, "Tester", "tester@example.com", "ROLE_USER");
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());

            User dbUser = new User();
            dbUser.setId(101L);
            dbUser.setRole(com.itc.funkart.user.entity.Role.ROLE_USER);

            when(userService.findById(any())).thenReturn(Optional.of(dbUser));

            // FIX: Controller calls userMapper.toDto() for /me, not toResponse()
            UserDto userDto = new UserDto(101L, "Tester","tester@example.com", "ROLE_USER");
            when(userMapper.toDto(any())).thenReturn(userDto);
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