package com.itc.funkart.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.user.config.ApiConfig;
import com.itc.funkart.user.config.WebConfig;
import com.itc.funkart.user.dto.event.UserLoginEvent;
import com.itc.funkart.user.dto.user.JwtUserDto;
import com.itc.funkart.user.dto.user.LoginRequest;
import com.itc.funkart.user.dto.user.SignupRequest;
import com.itc.funkart.user.dto.user.SuccessfulLoginResponse;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.AlreadyExistsException;
import com.itc.funkart.user.exceptions.GlobalExceptionHandler;
import com.itc.funkart.user.mapper.UserMapper;
import com.itc.funkart.user.service.JwtService;
import com.itc.funkart.user.service.KafkaEventPublisher;
import com.itc.funkart.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exhaustive Web-tier tests for {@link UserController}.
 * <p>Ensures that routing, global path prefixing, and exception handling
 * work in harmony across all authentication endpoints.</p>
 */
@WebMvcTest(UserController.class)
@Import({GlobalExceptionHandler.class, WebConfig.class, ApiConfig.class})
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;
    @MockBean private UserMapper userMapper;
    @MockBean private JwtService jwtService;
    @MockBean private KafkaEventPublisher kafkaEventPublisher;

    @Nested
    @DisplayName("Registration & Login Suites")
    class AuthenticationFlows {

        /**
         * Verifies a perfect signup flow including event publishing.
         */
        @Test
        @DisplayName("POST /signup - Success path with status 201")
        void signup_Success() throws Exception {
            SignupRequest request = new SignupRequest("test@test.com", "password123", "Test User");
            User user = User.builder().id(1L).email("test@test.com").name("Test User").build();
            SuccessfulLoginResponse resp = new SuccessfulLoginResponse(1L, "test@test.com", "Test User", "jwt");

            when(userService.signUp(any())).thenReturn(user);
            when(jwtService.generateJwtToken(any())).thenReturn("jwt");
            when(userMapper.toResponse(any(), anyString())).thenReturn(resp);

            mockMvc.perform(post("/api/v1/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Signup successful"));

            verify(kafkaEventPublisher).publishUserSignupEvent(any());
        }

        @Test
        @DisplayName("POST /login - Successful login publishes event and returns JWT")
        void login_Success() throws Exception {
            LoginRequest request = new LoginRequest("test@test.com", "password123");
            User mockUser = User.builder().id(1L).email("test@test.com").name("Tester").build();
            String mockJwt = "mock.jwt.token";

            // 1. Mock Service Logic
            when(userService.login(any(LoginRequest.class))).thenReturn(mockUser);
            when(jwtService.generateJwtToken(any(JwtUserDto.class))).thenReturn(mockJwt);
            when(userMapper.toResponse(eq(mockUser), eq(mockJwt)))
                    .thenReturn(new SuccessfulLoginResponse(1L, "test@test.com", "Tester", mockJwt));

            // 2. Perform Request
            mockMvc.perform(post("/api/v1/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.token").value(mockJwt));

            // 3. VERIFY KAFKA CALL (Crucial for 100% line coverage)
            verify(kafkaEventPublisher, times(1)).publishUserLoginEvent(any(UserLoginEvent.class));
        }

        /**
         * Verifies that the {@link GlobalExceptionHandler} correctly maps business exceptions.
         */
        @Test
        @DisplayName("POST /signup - Returns 409 Conflict on duplicate email")
        void signup_Duplicate_Returns409() throws Exception {
            // Password must be 8+ chars to pass validation!
            SignupRequest request = new SignupRequest("dup@test.com", "password123", "Name");

            when(userService.signUp(any())).thenThrow(new AlreadyExistsException("Email taken"));

            mockMvc.perform(post("/api/v1/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict()) // Now this will be reached
                    .andExpect(jsonPath("$.error.message").value("Email taken"));
        }
    }

    @Nested
    @DisplayName("OAuth & Profile Edge Cases")
    class EdgeCaseFlows {

        /**
         * Tests the early-return logic when the frontend fails to provide a code.
         */
        @Test
        @DisplayName("POST /oauth/github - Returns 400 when code is null/missing")
        void github_NoCode_Returns400() throws Exception {
            Map<String, String> body = new HashMap<>(); // Body without "code" key

            mockMvc.perform(post("/api/v1/users/oauth/github")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }

        /**
         * Tests retrieval of the current user profile from the Security Context.
         */
        @Test
        @DisplayName("GET /me - Successfully retrieves current user from context")
        void getMe_Success() throws Exception {
            // 1. Setup Data
            JwtUserDto principal = new JwtUserDto(1L, "Tester", "test@test.com");
            User dbUser = User.builder().id(1L).email("test@test.com").name("Tester").build();
            SuccessfulLoginResponse expectedResponse = new SuccessfulLoginResponse(1L, "test@test.com", "Tester", null);

            // 2. Mock Security Context
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 3. Mock service and mapper
            when(userService.findById(1L)).thenReturn(Optional.of(dbUser));

            // CRITICAL: This ensures resp is not null!
            when(userMapper.toResponse(eq(dbUser), any())).thenReturn(expectedResponse);

            // 4. Perform & Assert
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value("test@test.com")) // $.data now exists!
                    .andExpect(jsonPath("$.message").value("User fetched successfully"));

            SecurityContextHolder.clearContext();
        }
    }
}