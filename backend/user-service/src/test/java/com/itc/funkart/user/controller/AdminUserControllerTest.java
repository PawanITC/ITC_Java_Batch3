package com.itc.funkart.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.common.dto.security.UserPrincipalDto;
import com.itc.funkart.common.dto.user.UserDto;
import com.itc.funkart.user.auth.JwtService;
import com.itc.funkart.user.auth.PrincipalFactory;
import com.itc.funkart.user.config.JwtConfig;
import com.itc.funkart.user.config.SecurityConfig;
import com.itc.funkart.user.dto.user.RoleUpdateDto;
import com.itc.funkart.user.dto.user.UserAdminSummary;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.BadRequestException;
import com.itc.funkart.user.mapper.UserMapper;
import com.itc.funkart.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * <h2>Admin User Controller Integration Test</h2>
 *
 * <p>
 * This class provides unit and integration tests for the {@link AdminUserController} layer.
 * It utilizes {@code @WebMvcTest} to perform sliced testing of the Web layer without
 * initializing the full application context or a real database.
 * </p>
 *
 * <p><b>Testing Strategy:</b></p>
 * <ul>
 * <li><b>Mocking:</b> External dependencies like {@link UserService} and {@link UserMapper}
 * are mocked using {@code @MockitoBean} to isolate controller logic.</li>
 * <li><b>Security:</b> Uses {@code @WithMockUser} and manual {@code Authentication}
 * injection to verify {@code @PreAuthorize} and role-based access control.</li>
 * <li><b>Validation:</b> Ensures that {@code RoleUpdateDto} constraints (Regex/NotBlank)
 * are enforced before reaching the service layer.</li>
 * </ul>
 *
 * <p><b>Key Guardrail Testing:</b>
 * Specifically validates that administrative users cannot perform "Self-Demotion"
 * by checking identity metadata against target user IDs.
 * </p>
 *
 * @author Abbas Gure
 * @version 1.1
 * @see AdminUserController
 */
@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, PrincipalFactory.class})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtConfig jwtConfig;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    private UserPrincipalDto mockPrincipal;

    @BeforeEach
    void setUp() {
        mockPrincipal = UserPrincipalDto.builder()
                .userId(1L)
                .email("admin@funkart.com")
                .name("Admin User")
                .role("ROLE_ADMIN")
                .build();
    }

    /**
     * Test: Successful retrieval of all users.
     * <p>Verifies that the endpoint returns a 200 OK and correctly maps
     * entity lists to administrative summary DTOs.</p>
     */
    @Test
    @DisplayName("GET /admin/users - Success")
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_ShouldReturnList() throws Exception {
        // Arrange
        User user = new User();
        UserAdminSummary summary = new UserAdminSummary(1L, "Test User", "user@test.com", "ROLE_USER", true);

        when(userService.findAllUsers()).thenReturn(List.of(user));
        when(userMapper.toAdminSummary(any())).thenReturn(summary);

        // Act & Assert
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("user@test.com"));
    }

    /**
     * Test: Successful role update for a third-party user.
     * <p>Verifies that when an Admin (ID 1) updates a different User (ID 2),
     * the request succeeds and the security guardrail is bypassed.</p>
     */
    @Test
    @DisplayName("PATCH /role - Success (Admin updating another user)")
    @WithMockUser(roles = "ADMIN")
    void updateUserRole_Success() throws Exception {
        Long targetUserId = 2L;
        RoleUpdateDto request = new RoleUpdateDto("ROLE_MODERATOR");
        User updatedUser = new User();
        UserDto responseDto = new UserDto(targetUserId, "Other User", "other@test.com", "ROLE_MODERATOR");

        // 1. ADD THE ROLE HERE!
        // SimpleGrantedAuthority is what Spring Security looks for.
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));

        var auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                mockPrincipal, null, authorities); // <--- Pass the authorities here

        when(userService.changeUserRole(eq(targetUserId), eq("ROLE_MODERATOR"), eq("admin@funkart.com")))
                .thenReturn(updatedUser);
        when(userMapper.toDto(any())).thenReturn(responseDto);

        mockMvc.perform(patch("/admin/users/" + targetUserId + "/role")
                        .with(csrf())
                        .with(authentication(auth)) // Now this auth object has the 'ADMIN' role
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_MODERATOR"));
    }

    /**
     * Test: Bean Validation on request body.
     * <p>Verifies that the {@code @Pattern} annotation in {@code RoleUpdateDto}
     * triggers a 400 Bad Request when an invalid role string is provided.</p>
     */
    @Test
    @DisplayName("PATCH /role - Validation Failure (Invalid Role Name)")
    @WithMockUser(roles = "ADMIN")
    void updateUserRole_InvalidRole_ShouldReturnBadRequest() throws Exception {
        // Arrange: "GOD_MODE" is not in the @Pattern regex
        RoleUpdateDto request = new RoleUpdateDto("GOD_MODE");

        // Act & Assert
        mockMvc.perform(patch("/admin/users/1/role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test: Role-Based Access Control (RBAC).
     * <p>Verifies that users with {@code ROLE_USER} are denied access (403 Forbidden)
     * to administrative endpoints as per the {@code @PreAuthorize} configuration.</p>
     */
    @Test
    @DisplayName("GET /admin/users - Forbidden for ROLE_USER")
    @WithMockUser(roles = "USER")
    void getAllUsers_AsUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    /**
     * Test: Self-Demotion Guardrail.
     * <p>Verifies that the system throws a 400 Bad Request if an administrator
     * attempts to change their own role to something other than ROLE_ADMIN.</p>
     */
    @Test
    @DisplayName("PATCH /role - Failure (Admin attempting self-demotion)")
    @WithMockUser(roles = "ADMIN")
    void updateUserRole_SelfDemotion_ShouldFail() throws Exception {
        // Arrange: Admin (ID 1) trying to update themselves (ID 1)
        Long adminId = 1L;
        RoleUpdateDto request = new RoleUpdateDto("ROLE_USER");

        // The authorities must include ROLE_ADMIN to pass @PreAuthorize
        var authorities = List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"));

        // Use mockPrincipal (which has email "admin@funkart.com")
        var auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                mockPrincipal, null, authorities);

        // Mock the service to throw the exception when the emails match
        when(userService.changeUserRole(eq(adminId), eq("ROLE_USER"), eq("admin@funkart.com")))
                .thenThrow(new BadRequestException("Security Violation: You cannot remove your own administrative privileges."));

        // Act & Assert
        mockMvc.perform(patch("/admin/users/" + adminId + "/role")
                        .with(csrf())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                // Verify the error message if your GlobalExceptionHandler returns it
                .andExpect(jsonPath("$.error.message").value("Security Violation: You cannot remove your own administrative privileges."));
    }
}
