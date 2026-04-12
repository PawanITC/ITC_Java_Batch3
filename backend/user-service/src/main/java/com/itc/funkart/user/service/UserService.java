package com.itc.funkart.user.service;

import com.itc.funkart.user.dto.user.LoginRequest;
import com.itc.funkart.user.dto.user.SignupRequest;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.AlreadyExistsException;
import com.itc.funkart.user.exceptions.BadRequestException;
import com.itc.funkart.user.exceptions.UnauthorizedException;
import com.itc.funkart.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service class handling core user operations such as registration, authentication,
 * and profile retrieval. Links directly with {@link UserRepository} for data persistence.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new user in the system after validating input and checking for duplicates.
     * * @param request The {@link SignupRequest} containing {@code email}, {@code password}, and {@code name}.
     * @return The newly persisted {@link User} entity.
     * @throws AlreadyExistsException if the email is already in use.
     */
    public User signUp(SignupRequest request) {
        validateSignupInput(request.email(), request.password(), request.name());
        checkEmailExists(request.email());

        User newUser = User.builder()
                .email(request.email())
                .password(hashPassword(request.password()))
                .name(request.name())
                .build();

        return userRepository.save(newUser);
    }

    /**
     * Authenticates a user based on email and password.
     * * @param request The {@link LoginRequest} containing {@code email} and {@code password}.
     * @return The authenticated {@link User} entity.
     * @throws UnauthorizedException if credentials do not match.
     */
    public User login(LoginRequest request) {
        validateLoginInput(request.email(), request.password());

        User user = fetchUserByEmail(request.email());
        validatePassword(request.password(), user.getPassword());

        return user;
    }

    // ------------------------
    // Validation Methods
    // ------------------------

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

    // ------------------------
    // Helper Methods
    // ------------------------

    /**
     * Verifies if an email is already registered in the database.
     * @param email The {@code email} to check.
     */
    private void checkEmailExists(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new AlreadyExistsException("Email already registered");
        }
    }

    /**
     * Compares a raw password against a Bcrypt encoded hash.
     * @param rawPassword The plaintext password from the user.
     * @param storedPassword The hashed password from the database.
     */
    private void validatePassword(String rawPassword, String storedPassword) {
        if (!passwordEncoder.matches(rawPassword, storedPassword)) {
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    /**
     * Encrypts a plaintext password.
     * @param rawPassword The password to hash.
     * @return The {@code BCrypt} encoded string.
     */
    private String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Retrieves a user by email or throws an unauthorized exception.
     */
    private User fetchUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
    }

    /**
     * Finds a user by their primary key.
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Finds a user by email (Safe for OAuth flow).
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Creates a new user record, typically used for OAuth registrations.
     * * @param email The user's email address.
     * @param password The password (can be {@code null} for OAuth).
     * @param name The user's display name.
     * @return The persisted {@link User}.
     */
    public User createUser(String email, String password, String name) {
        User user = User.builder()
                .email(email)
                .password(password)
                .name(name)
                .build();
        return userRepository.save(user);
    }
}