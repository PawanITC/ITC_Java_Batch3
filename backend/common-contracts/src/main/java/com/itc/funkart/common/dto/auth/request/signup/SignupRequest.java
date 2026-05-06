package com.itc.funkart.common.dto.auth.request.signup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Request payload for creating a new user account.
 */
@Builder
@Jacksonized
public record SignupRequest(
        /* The user's full display name. */
        @NotBlank(message = "Name is required")
        @Size(max = 50)
        String name,

        /* The unique email address for the account. */
        @NotBlank(message = "Email is required")
        @Email
        String email,

        /* The plain-text password (to be encoded by User-Service). */
        @NotBlank(message = "Password is required")
        @Size(min = 8)
        String password
) {}