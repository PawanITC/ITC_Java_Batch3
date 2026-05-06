package com.itc.funkart.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.common.dto.security.UserPrincipalDto;
import com.itc.funkart.common.dto.user.UserDto;
import com.itc.funkart.common.dto.user.UserProfileDto;
import com.itc.funkart.user.auth.AuthFacadeService;
import com.itc.funkart.user.dto.user.*;
import com.itc.funkart.user.exceptions.AlreadyExistsException;
import com.itc.funkart.user.exceptions.GlobalExceptionHandler;
import com.itc.funkart.user.exceptions.UnauthorizedException;
import com.itc.funkart.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <h2>UserController — Web Layer Tests</h2>
 *
 * <p>Tests the HTTP routing and response shaping of {@link UserController}
 * in isolation. Security filters are disabled via
 * {@code @AutoConfigureMockMvc(addFilters = false)} so tests focus purely on
 * controller behaviour.
 *
 * <p><b>Key fixes from old tests:</b>
 * <ul>
 *   <li>The controller uses {@link AuthFacadeService} for auth flows — NOT
 *       {@code UserService} + {@code JwtService} + {@code UserMapper} directly.
 *       Mocking the wrong layer caused all interaction assertions to be silent no-ops.</li>
 *   <li>{@code GET /me} calls {@code userService.getUserProfile(id)} returning
 *       {@link UserProfileDto} — the old tests mocked {@code userMapper.toDto} +
 *       {@code userService.findById}, which the controller never calls.</li>
 *   <li>The {@code @AuthenticationPrincipal} type is {@link UserPrincipalDto} —
 *       not {@code JwtUserDto}.</li>
 *   <li>Endpoint paths are relative to {@code /users} with NO {@code /api/v1} prefix
 *       in a {@code @WebMvcTest} slice unless {@code WebConfig} is explicitly imported.</li>
 * </ul>
 */
@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * The controller's two direct dependencies — AuthFacadeService and UserService.
     * Nothing else is called from the controller layer.
     */
    @MockitoBean
    private AuthFacadeService authFacadeService;
    @MockitoBean
    private UserService userService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        reset(authFacadeService, userService);
    }

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * Builds a standard success response for reuse.
     */
    private SuccessfulLoginResponse loginResponse(String email, String token) {
        UserDto user = new UserDto(1L, "Alice", email, "ROLE_USER");
        return new SuccessfulLoginResponse(user, token);
    }

    // -------------------------------------------------------------------------
    // POST /users/signup
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /users/signup")
    class SignupTests {

        @Test
        @DisplayName("Returns 201 Created on successful signup")
        void signup_returns201() throws Exception {
            when(authFacadeService.signup(any(SignupRequest.class)))
                    .thenReturn(loginResponse("alice@example.com", "jwt-token"));

            mockMvc.perform(post("/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new SignupRequest("Alice", "alice@example.com", "password123"))))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Response body contains message 'Signup successful'")
        void signup_responseMessage() throws Exception {
            when(authFacadeService.signup(any()))
                    .thenReturn(loginResponse("alice@example.com", "jwt-token"));

            mockMvc.perform(post("/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new SignupRequest("Alice", "alice@example.com", "password123"))))
                    .andExpect(jsonPath("$.message").value("Signup successful"));
        }

        @Test
        @DisplayName("Response body contains user email")
        void signup_responseContainsEmail() throws Exception {
            when(authFacadeService.signup(any()))
                    .thenReturn(loginResponse("alice@example.com", "jwt-token"));

            mockMvc.perform(post("/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new SignupRequest("Alice", "alice@example.com", "password123"))))
                    .andExpect(jsonPath("$.data.user.email").value("alice@example.com"));
        }

        @Test
        @DisplayName("Response body contains token")
        void signup_responseContainsToken() throws Exception {
            when(authFacadeService.signup(any()))
                    .thenReturn(loginResponse("alice@example.com", "my-jwt"));

            mockMvc.perform(post("/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new SignupRequest("Alice", "alice@example.com", "password123"))))
                    .andExpect(jsonPath("$.data.token").value("my-jwt"));
        }

        @Test
        @DisplayName("Delegates to AuthFacadeService.signup")
        void signup_delegatesToFacade() throws Exception {
            when(authFacadeService.signup(any()))
                    .thenReturn(loginResponse("alice@example.com", "jwt"));

            mockMvc.perform(post("/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new SignupRequest("Alice", "alice@example.com", "password123"))))
                    .andExpect(status().isCreated());

            verify(authFacadeService).signup(any(SignupRequest.class));
        }

        @Test
        @DisplayName("Returns 409 Conflict when email is already registered")
        void signup_returns409OnDuplicateEmail() throws Exception {
            when(authFacadeService.signup(any()))
                    .thenThrow(new AlreadyExistsException("Email already registered"));

            mockMvc.perform(post("/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new SignupRequest("Alice", "dup@example.com", "password123"))))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.error.field").value("email"));
        }

        @Test
        @DisplayName("Returns 400 when request body fails bean validation (blank name)")
        void signup_returns400OnValidationFailure() throws Exception {
            mockMvc.perform(post("/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new SignupRequest("", "alice@example.com", "password123"))))
                    .andExpect(status().isBadRequest());
        }
    }

    // -------------------------------------------------------------------------
    // POST /users/login
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /users/login")
    class LoginTests {

        @Test
        @DisplayName("Returns 200 OK on successful login")
        void login_returns200() throws Exception {
            when(authFacadeService.login(any(LoginRequest.class)))
                    .thenReturn(loginResponse("alice@example.com", "jwt-token"));

            mockMvc.perform(post("/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new LoginRequest("alice@example.com", "password123"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Response body contains user name")
        void login_responseContainsName() throws Exception {
            when(authFacadeService.login(any()))
                    .thenReturn(loginResponse("alice@example.com", "jwt"));

            mockMvc.perform(post("/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new LoginRequest("alice@example.com", "password123"))))
                    .andExpect(jsonPath("$.data.user.name").value("Alice"));
        }

        @Test
        @DisplayName("Response body contains token")
        void login_responseContainsToken() throws Exception {
            when(authFacadeService.login(any()))
                    .thenReturn(loginResponse("alice@example.com", "login-jwt"));

            mockMvc.perform(post("/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new LoginRequest("alice@example.com", "password123"))))
                    .andExpect(jsonPath("$.data.token").value("login-jwt"));
        }

        @Test
        @DisplayName("Returns 401 when credentials are invalid")
        void login_returns401OnBadCredentials() throws Exception {
            when(authFacadeService.login(any()))
                    .thenThrow(new UnauthorizedException("Invalid email or password"));

            mockMvc.perform(post("/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new LoginRequest("alice@example.com", "wrong"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("Returns 400 when email fails bean validation")
        void login_returns400OnInvalidEmail() throws Exception {
            mockMvc.perform(post("/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new LoginRequest("not-an-email", "password123"))))
                    .andExpect(status().isBadRequest());
        }
    }

    // -------------------------------------------------------------------------
    // POST /users/oauth/github
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /users/oauth/github")
    class OAuthGithubTests {

        @Test
        @DisplayName("Returns 200 OK with OAuthResponse on successful code exchange")
        void oauth_returns200() throws Exception {
            UserDto userDto = new UserDto(1L, "Alice", "alice@example.com", "ROLE_USER");
            OAuthResponse oauthResp = new OAuthResponse(userDto, "oauth-jwt");
            when(authFacadeService.handleGithubLogin("valid-code")).thenReturn(oauthResp);

            mockMvc.perform(post("/users/oauth/github")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new OAuthRequest("valid-code"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("GitHub login successful"));
        }

        @Test
        @DisplayName("Response contains user email from GitHub profile")
        void oauth_responseContainsEmail() throws Exception {
            UserDto userDto = new UserDto(1L, "Alice", "alice@example.com", "ROLE_USER");
            OAuthResponse oauthResp = new OAuthResponse(userDto, "oauth-jwt");
            when(authFacadeService.handleGithubLogin("valid-code")).thenReturn(oauthResp);

            mockMvc.perform(post("/users/oauth/github")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new OAuthRequest("valid-code"))))
                    .andExpect(jsonPath("$.data.user.email").value("alice@example.com"));
        }

        @Test
        @DisplayName("Returns 400 when code is blank (bean validation)")
        void oauth_returns400WhenCodeBlank() throws Exception {
            mockMvc.perform(post("/users/oauth/github")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new OAuthRequest(""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.field").value("code"));
        }
    }

    // -------------------------------------------------------------------------
    // GET /users/me
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /users/me")
    class GetMeTests {

        /**
         * Injects a {@link UserPrincipalDto} into the SecurityContext so
         * {@code @AuthenticationPrincipal} resolves correctly in the controller.
         */
        private void authenticate(Long id, String email) {
            // Ensuring the principal matches the type expected by the Controller
            UserPrincipalDto principal = UserPrincipalDto.builder()
                    .userId(id)
                    .name("Alice")
                    .email(email)
                    .role("ROLE_USER")
                    .build();

            var auth = new UsernamePasswordAuthenticationToken(
                    principal, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        @Test
        @DisplayName("Returns 200 OK with profile when authenticated")
        void getMe_returns200() throws Exception {
            authenticate(1L, "alice@example.com");
            when(userService.getUserProfile(1L))
                    .thenReturn(new UserProfileDto(1L, "Alice", "alice@example.com", "ROLE_USER"));

            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Response contains user email from profile")
        void getMe_responseContainsEmail() throws Exception {
            authenticate(1L, "alice@example.com");
            when(userService.getUserProfile(1L))
                    .thenReturn(new UserProfileDto(1L, "Alice", "alice@example.com", "ROLE_USER"));

            mockMvc.perform(get("/users/me"))
                    .andExpect(jsonPath("$.data.email").value("alice@example.com"));
        }

        @Test
        @DisplayName("Response contains message 'User profile fetched successfully'")
        void getMe_responseMessage() throws Exception {
            authenticate(1L, "alice@example.com");
            when(userService.getUserProfile(1L))
                    .thenReturn(new UserProfileDto(1L, "Alice", "alice@example.com", "ROLE_USER"));

            mockMvc.perform(get("/users/me"))
                    .andExpect(jsonPath("$.message").value("User profile fetched successfully"));
        }

        @Test
        @DisplayName("Delegates to UserService.getUserProfile with the correct userId")
        void getMe_delegatesToUserService() throws Exception {
            authenticate(42L, "alice@example.com");
            when(userService.getUserProfile(42L))
                    .thenReturn(new UserProfileDto(42L, "Alice", "alice@example.com", "ROLE_USER"));

            mockMvc.perform(get("/users/me"));

            verify(userService).getUserProfile(42L);
        }

        @Test
        @DisplayName("Returns 401 when no principal is present")
        void getMe_returns401WithoutPrincipal() throws Exception {
            // No authentication set in context
            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isUnauthorized());
        }
    }
}