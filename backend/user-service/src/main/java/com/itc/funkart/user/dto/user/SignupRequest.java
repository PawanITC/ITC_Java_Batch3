package com.itc.funkart.user.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Payload required to register a new user via the standard email/password flow.
 * <p>
 * This record is used for the initial registration step. Note that administrative
 * roles cannot be requested via this payload; all signups default to {@code ROLE_USER}
 * at the service level for security.
 * </p>
 *
 * @param name     User's display name.
 * @param email    Validated email string. Must be unique in the system.
 * @param password Plaintext password. Encrypted using Bcrypt before persistence.
 */
@Builder
public record SignupRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 50, message = "Name must be less than 50 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password

) {}