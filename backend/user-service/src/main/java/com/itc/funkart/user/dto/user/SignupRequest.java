package com.itc.funkart.user.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Payload required to register a new user via the standard email/password flow.
 * * @param email    Validated email string.
 * @param password Plaintext password (min 8 chars).
 * @param name     User's full name (max 50 chars).
 */
@Builder
public record SignupRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Name is required")
        @Size(max = 50, message = "Name must be less than 50 characters")
        String name
) {}
