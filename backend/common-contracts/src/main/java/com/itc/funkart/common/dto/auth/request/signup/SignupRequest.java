package com.itc.funkart.common.dto.auth.request.signup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Request payload for creating a new user account.
 *
 * @param name     The user's full display name. Maximum 50 characters.
 * @param email    The unique email address for the account. Must be RFC-compliant.
 * @param password The plain-text password (minimum 8 characters); BCrypt-encoded by User-Service.
 */
@Builder
@Jacksonized
public record SignupRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 50)
        String name,

        @NotBlank(message = "Email is required")
        @Email
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8)
        String password
) {
}