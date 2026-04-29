package com.itc.funkart.user.controller;

import com.itc.funkart.user.dto.security.UserPrincipalDto;
import com.itc.funkart.user.dto.user.RoleUpdateDto;
import com.itc.funkart.user.dto.user.UserAdminSummary;
import com.itc.funkart.user.dto.user.UserDto;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.mapper.UserMapper;
import com.itc.funkart.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <h2>Administrative User Management Controller</h2>
 * <p>Provides elevated endpoints for system administrators to manage user accounts,
 * modify authorization levels, and monitor account statuses across the Funkart system.</p>
 *
 * @author Abbas Gure
 * @version 1.1
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminUserController {

    private final UserService userService;
    private final UserMapper userMapper;

    /**
     * Retrieves a comprehensive list of all registered users.
     * <p>Maps core {@link User} entities to {@link UserAdminSummary} DTOs,
     * which include administrative metadata like account status (is_active).</p>
     *
     * @return {@link ResponseEntity} containing a list of all users formatted for admin view.
     */
    @GetMapping
    public ResponseEntity<List<UserAdminSummary>> getAllUsers() {
        log.info("Admin request: Fetching all users for dashboard");

        List<User> users = userService.findAllUsers();

        List<UserAdminSummary> summaryList = users.stream()
                .map(userMapper::toAdminSummary)
                .toList();

        return ResponseEntity.ok(summaryList);
    }

    /**
     * Updates the security role of a specific user.
     * <p>Allows promotion or demotion of users. Includes a safety check to prevent
     * the acting administrator from removing their own administrative access.</p>
     *
     * @param userId         The unique database identifier of the user to update.
     * @param request        Data transfer object containing the target role.
     * @param authentication The current security context.
     * @return               {@link ResponseEntity} containing the updated {@link UserDto}.
     */
    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserDto> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody RoleUpdateDto request,
            Authentication authentication) {

        log.info("Admin request: Updating role for user ID: {} to {}", userId, request.role());

        if (!(authentication.getPrincipal() instanceof UserPrincipalDto principal)) {
            log.error("Principal type mismatch: Expected UserPrincipalDto");
            throw new IllegalStateException("Unexpected principal type in SecurityContext");
        }

        User updatedUser = userService.changeUserRole(userId, request.role(), principal.email());

        return ResponseEntity.ok(userMapper.toDto(updatedUser));
    }
}