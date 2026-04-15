package com.itc.funkart.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.user.config.ApiConfig;
import com.itc.funkart.user.config.WebConfig;
import com.itc.funkart.user.dto.event.UserLoginEvent;
import com.itc.funkart.user.dto.user.*;
import com.itc.funkart.user.entity.Role;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.AlreadyExistsException;
import com.itc.funkart.user.exceptions.GlobalExceptionHandler;
import com.itc.funkart.user.mapper.UserMapper;
import com.itc.funkart.user.service.GithubOAuthService;
import com.itc.funkart.user.service.JwtService;
import com.itc.funkart.user.service.KafkaEventPublisher;
import com.itc.funkart.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exhaustive Web-tier tests for {@link UserController}.
 * <p>Ensures that routing, global path prefixing, and exception handling
 * work in harmony across all authentication endpoints.</p>
 */
@WebMvcTest(UserController.class)
@EnableConfigurationProperties(ApiConfig.class)
@Import({GlobalExceptionHandler.class, WebConfig.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ApiConfig apiConfig;

    @MockitoBean
    private GithubOAuthService githubOAuthService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private UserMapper userMapper;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private KafkaEventPublisher kafkaEventPublisher;

    @BeforeEach
    void setUp() {
        // This clears the "memory" of your mocks so verify(times(1))
        // doesn't count calls from previous tests.
        reset(userService, jwtService, userMapper, kafkaEventPublisher, githubOAuthService);

        // Also clear the security context to prevent getMe_Success pollution
        SecurityContextHolder.clearContext();
    }

    /**
     * Helper to stay in sync with WebConfig versioning.
     * Maps the API prefix and version dynamically from configuration.
     * * @param path The endpoint path relative to the users' controller.
     *
     * @return The fully qualified URL string.
     */
    private String getUrl(String path) {
        // WebConfig does: "/api/" + version
        return "/api/" + apiConfig.getVersion() + "/users" + path;
    }

    @Nested
    @DisplayName("Registration & Login Suites")
    class AuthenticationFlows {

        /**
         * Verifies a perfect signup flow including event publishing.
         */
        @Test
        @DisplayName("POST /signup - Success path with status 201")
        void signup_Success() throws Exception {
            SignupRequest request = new SignupRequest("Test User","test@test.com", "password123");
            User user = User.builder().id(1L).email("test@test.com").name("Test User").role(Role.ROLE_USER).build();

            // Updated DTO: Assuming SuccessfulLoginResponse(UserDto user, String token) - retain dto vals order
            UserDto userDto = new UserDto(1L, "Test User", "test@test.com", "ROLE_USER");
            SuccessfulLoginResponse resp = new SuccessfulLoginResponse(userDto, "jwt");

            when(userService.signUp(any())).thenReturn(user);
            when(jwtService.generateJwtToken(any())).thenReturn("jwt");
            when(userMapper.toResponse(any(), anyString())).thenReturn(resp);

            mockMvc.perform(post(getUrl("/signup"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Signup successful"))
                    // Note the path: $.data.user.email
                    .andExpect(jsonPath("$.data.user.email").value("test@test.com"))
                    .andExpect(jsonPath("$.data.token").value("jwt"));

            verify(kafkaEventPublisher).publishUserSignupEvent(any());
        }

        @Test
        @DisplayName("POST /login - Successful login returns nested User and Token")
        void login_Success() throws Exception {
            LoginRequest request = new LoginRequest("test@test.com", "password123");
            User mockUser = User.builder().id(1L).email("test@test.com").name("Tester").role(Role.ROLE_USER).build();
            String mockJwt = "mock.jwt.token";

            UserDto userDto = new UserDto(1L, "Tester", "test@test.com", "ROLE_USER");
            SuccessfulLoginResponse resp = new SuccessfulLoginResponse(userDto, mockJwt);

            when(userService.login(any(LoginRequest.class))).thenReturn(mockUser);
            when(jwtService.generateJwtToken(any(JwtUserDto.class))).thenReturn(mockJwt);
            when(userMapper.toResponse(eq(mockUser), eq(mockJwt))).thenReturn(resp);

            mockMvc.perform(post(getUrl("/login"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.user.name").value("Tester"))
                    .andExpect(jsonPath("$.data.token").value(mockJwt));

            verify(kafkaEventPublisher, times(1)).publishUserLoginEvent(any(UserLoginEvent.class));
        }

        /**
         * Verifies that the {@link GlobalExceptionHandler} correctly maps business exceptions.
         */
        @Test
        @DisplayName("POST /signup - Returns 409 Conflict on duplicate email")
        void signup_Duplicate_Returns409() throws Exception {
            SignupRequest request = new SignupRequest("Name","dup@test.com", "password123");
            when(userService.signUp(any())).thenThrow(new AlreadyExistsException("Email already registered"));

            mockMvc.perform(post(getUrl("/signup"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    // GlobalExceptionHandler maps AlreadyExistsException to CONFLICT
                    .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.error.field").value("email"));
        }
    }

    @Nested
    @DisplayName("OAuth & Profile Edge Cases")
    class EdgeCaseFlows {

        /**
         * Tests the early-return logic when the frontend fails to provide a code.
         */
        @Test
        @DisplayName("POST /oauth/github - Returns 400 when code is empty")
        void github_NoCode_Returns400() throws Exception {
            // Updated to use the new OAuthRequest record structure
            OAuthRequest request = new OAuthRequest("");

            mockMvc.perform(post(getUrl("/oauth/github"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.field").value("code"));
        }

        /**
         * Tests retrieval of the current user profile from the Security Context.
         */
        @Test
        @DisplayName("GET /me - Successfully retrieves current user from context")
        void getMe_Success() throws Exception {
            // Updated JwtUserDto constructor (ID, Name, Email, Role)
            JwtUserDto principal = new JwtUserDto(1L, "Tester", "test@test.com", "ROLE_USER");
            User dbUser = User.builder().id(1L).email("test@test.com").name("Tester").build();
            UserDto userDto = new UserDto(1L, "Tester", "test@test.com", "ROLE_USER");

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);

            when(userService.findById(1L)).thenReturn(Optional.of(dbUser));
            when(userMapper.toDto(dbUser)).thenReturn(userDto);

            mockMvc.perform(get(getUrl("/me")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value("test@test.com"))
                    .andExpect(jsonPath("$.message").value("User fetched successfully"));

            SecurityContextHolder.clearContext();
        }
    }
}