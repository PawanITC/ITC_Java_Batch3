package com.itc.funkart.user.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Data transfer object representing a user's attempt to authenticate via email and password.
 * This record is validated at the controller level to ensure credential integrity.
 * * @param email    The user's registered email address. Must be a valid email format.
 * @param password The user's plaintext password. Cannot be blank.
 */
@Builder
public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {}