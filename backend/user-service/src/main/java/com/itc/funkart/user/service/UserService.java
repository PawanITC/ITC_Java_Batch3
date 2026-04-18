package com.itc.funkart.user.service;

import com.itc.funkart.user.dto.user.LoginRequest;
import com.itc.funkart.user.dto.user.SignupRequest;
import com.itc.funkart.user.dto.user.UserProfileDto;
import com.itc.funkart.user.entity.Role;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.AlreadyExistsException;
import com.itc.funkart.user.exceptions.BadRequestException;
import com.itc.funkart.user.exceptions.UnauthorizedException;
import com.itc.funkart.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

        return new UserProfileDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    /**
     * OAuth user creation (domain operation only).
     */
    public User createUser(String email, String password, String name) {
        return userRepository.save(User.builder()
                .name(name)
                .email(email)
                .password(password)
                .role(Role.ROLE_USER)
                .build());
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
    public User getOrCreateOAuthUser(String email, String name) {
        return userRepository.findByEmail(email)
                .orElseGet(() ->
                        userRepository.save(User.builder()
                                .email(email)
                                .name(name)
                                .password("{OAUTH}")
                                .role(Role.ROLE_USER)
                                .build()
                        )
                );
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
}