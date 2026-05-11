package com.itc.funkart.user.service;

import com.itc.funkart.common.dto.user.UserProfileDto;
import com.itc.funkart.user.dto.user.LoginRequest;
import com.itc.funkart.user.dto.user.OAuthUserResult;
import com.itc.funkart.user.dto.user.SignupRequest;
import com.itc.funkart.user.entity.Role;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.AlreadyExistsException;
import com.itc.funkart.user.exceptions.BadRequestException;
import com.itc.funkart.user.exceptions.UnauthorizedException;
import com.itc.funkart.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * <h2>User Service</h2>
 *
 * <p>
 * Core domain service responsible for user lifecycle operations:
 * persistence, validation, and retrieval.
 * </p>
 *
 * <p>
 * IMPORTANT:
 * This service does NOT generate JWTs or construct API response DTOs.
 * It only returns domain objects or simple projections.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaEventPublisher kafkaEventPublisher;


    /**
     * Creates a new user via email/password signup.
     *
     * @return persisted User entity
     */
    public User signUp(SignupRequest request) {

        validateSignupInput(request.email(), request.password(), request.name());
        checkEmailExists(request.email());

        User user = userRepository.save(User.builder()
                .name(request.name())
                .email(request.email())
                .password(hashPassword(request.password()))
                .role(Role.ROLE_USER)
                .build());

        kafkaEventPublisher.publishSignup(user);
        return user;
    }

    /**
     * Authenticates a user and returns domain User if valid.
     */
    public User login(LoginRequest request) {

        validateLoginInput(request.email(), request.password());

        User user = fetchUserByEmail(request.email());
        validatePassword(request.password(), user.getPassword());

        return user;
    }

    /**
     * Returns user profile projection.
     *
     * <p>
     * NOTE: This method returns a DTO because profile is a read-model concern,
     * not part of authentication/session handling.
     * </p>
     */
    public UserProfileDto getUserProfile(Long userId) {
        User user = findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean hasPassword = user.getPassword() != null && !"{OAUTH}".equals(user.getPassword());
        return new UserProfileDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                hasPassword
        );
    }


    public void recordLogin(User user, String method) {
        kafkaEventPublisher.publishLogin(user, method);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // ---------------- Idempotent Method ----------------

    @Transactional
    public OAuthUserResult getOrCreateOAuthUser(String email, String name) {

        Optional<User> existing = userRepository.findByEmail(email);

        if (existing.isPresent()) {
            return new OAuthUserResult(existing.get(), false);
        }

        User newUser = userRepository.save(User.builder()
                .email(email)
                .name(name)
                .password("{OAUTH}")
                .role(Role.ROLE_USER)
                .build()
        );

        return new OAuthUserResult(newUser, true);
    }

    // ---------------- validation ----------------

    private void validateSignupInput(String email, String password, String name) {
        emailAndPasswordCheck(email, password);
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
    }

    private void validateLoginInput(String email, String password) {
        emailAndPasswordCheck(email, password);
    }

    private void emailAndPasswordCheck(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required");
        }
        if (password == null || password.isBlank()) {
            throw new BadRequestException("Password is required");
        }
    }

    private void checkEmailExists(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new AlreadyExistsException("Email already registered");
        }
    }

    private void validatePassword(String raw, String encoded) {
        if (!passwordEncoder.matches(raw, encoded)) {
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    private String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    private User fetchUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
    }


    // Admin user service methods

    /**
     * Updates a user's role.
     * * @param targetId The ID of the user being promoted/demoted
     *
     * @param newRoleName The string representation of the role (e.g., "ROLE_ADMIN")
     * @param adminEmail  The email of the admin performing the action (for self-demotion check)
     * @return The updated User entity
     */
    @Transactional
    public User changeUserRole(Long targetId, String newRoleName, String adminEmail) {
        // 1. Fetch target user with a specific exception
        User targetUser = userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + targetId));

        // 2. Parse and Validate the new role
        Role requestedRole;
        try {
            requestedRole = Role.valueOf(newRoleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role type: " + newRoleName);
        }

        // 3. Performance Optimization: Skip update if the role is already the same
        if (targetUser.getRole() == requestedRole) {
            return targetUser;
        }

        // 4. THE GUARDRAIL: Prevent self-demotion
        // We compare emails case-insensitively to ensure "Admin@..." and "admin@..." match
        if (targetUser.getEmail().equalsIgnoreCase(adminEmail)) {
            if (requestedRole != Role.ROLE_ADMIN) {
                throw new BadRequestException("Security Violation: You cannot remove your own administrative privileges.");
            }
        }

        // 5. Apply changes
        log.info("Admin [{}] is changing role of user [{}] from [{}] to [{}]",
                adminEmail, targetUser.getEmail(), targetUser.getRole(), requestedRole);

        targetUser.setRole(requestedRole);

        // In @Transactional methods, save() is technically optional but good for clarity
        return userRepository.save(targetUser);
    }

    /**
     * Retrieves all registered users for administrative management.
     * Read-only transaction keeps the Hibernate session open during entity access,
     * preventing LazyInitializationException.
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<User> findAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Updates the display name of the authenticated user.
     */
    @Transactional
    public UserProfileDto updateProfile(Long userId, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new BadRequestException("Name cannot be blank");
        }
        User user = findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setName(newName.trim());
        user = userRepository.save(user);
        boolean hasPassword = user.getPassword() != null && !"{OAUTH}".equals(user.getPassword());
        return new UserProfileDto(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), hasPassword);
    }

    /**
     * Changes the user's password after verifying the current one.
     * OAuth-only accounts (password stored as "{OAUTH}") cannot use this method.
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("{OAUTH}".equals(user.getPassword())) {
            throw new BadRequestException("Password change is not available for OAuth accounts.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new BadRequestException("New password must be at least 8 characters.");
        }
        user.setPassword(hashPassword(newPassword));
        userRepository.save(user);
        log.info("Password changed for user {}", userId);
    }

    /**
     * Toggles the active/inactive status of a user account.
     * Admins cannot deactivate their own account.
     */
    @Transactional
    public User toggleUserActive(Long targetId, String adminEmail) {
        User targetUser = userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + targetId));

        if (targetUser.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new BadRequestException("Security Violation: You cannot deactivate your own account.");
        }

        targetUser.setActive(!targetUser.isActive());
        log.info("Admin [{}] set user [{}] active={}", adminEmail, targetUser.getEmail(), targetUser.isActive());
        return userRepository.save(targetUser);
    }
}